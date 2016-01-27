/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;

import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UnlockedController extends AbstractFXMLViewController {

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.25;
	private LockListener listener;
	private Vault vault;
	private Timeline ioAnimation;

	@FXML
	private Label messageLabel;

	@FXML
	private LineChart<Number, Number> ioGraph;

	@FXML
	private NumberAxis xAxis;

	private final Stage macWarningsWindow = new Stage();
	private final MacWarningsController macWarningsController;
	private final ExecutorService exec;

	@Inject
	public UnlockedController(Provider<MacWarningsController> macWarningsControllerProvider, ExecutorService exec) {
		this.macWarningsController = macWarningsControllerProvider.get();
		this.exec = exec;
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/unlocked.fxml");
	}

	@Override
	protected ResourceBundle getFxmlResourceBundle() {
		return ResourceBundle.getBundle("localization");
	}

	@Override
	public void initialize() {
		macWarningsController.initStage(macWarningsWindow);
		ActiveWindowStyleSupport.startObservingFocus(macWarningsWindow);
	}

	@FXML
	private void didClickRevealVault(ActionEvent event) {
		exec.submit(() -> {
			try {
				vault.reveal();
			} catch (CommandFailedException e) {
				Platform.runLater(() -> {
					messageLabel.setText(resourceBundle.getString("unlocked.label.revealFailed"));
				});
			}
		});
	}

	@FXML
	private void didClickCloseVault(ActionEvent event) {
		exec.submit(() -> {
			try {
				vault.unmount();
			} catch (CommandFailedException e) {
				Platform.runLater(() -> {
					messageLabel.setText(resourceBundle.getString("unlocked.label.unmountFailed"));
				});
				return;
			}
			vault.deactivateFrontend();
			if (listener != null) {
				Platform.runLater(() -> {
					listener.didLock(this);
				});
			}
		});
	}

	// ****************************************
	// MAC Auth Warnings
	// ****************************************

	private void macWarningsDidChange(ListChangeListener.Change<? extends String> change) {
		if (change.getList().size() > 0) {
			Platform.runLater(macWarningsWindow::show);
		} else {
			Platform.runLater(macWarningsWindow::hide);
		}
	}

	// ****************************************
	// IO Graph
	// ****************************************

	private void startIoSampling(final Object sampler) {
		final Series<Number, Number> decryptedBytes = new Series<>();
		decryptedBytes.setName("decrypted");
		final Series<Number, Number> encryptedBytes = new Series<>();
		encryptedBytes.setName("encrypted");

		ioGraph.getData().add(decryptedBytes);
		ioGraph.getData().add(encryptedBytes);

		ioAnimation = new Timeline();
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(sampler, decryptedBytes, encryptedBytes)));
		ioAnimation.setCycleCount(Animation.INDEFINITE);
		ioAnimation.play();
	}

	private void stopIoSampling() {
		if (ioAnimation != null) {
			ioGraph.getData().clear();
			ioAnimation.stop();
		}
	}

	private class IoSamplingAnimationHandler implements EventHandler<ActionEvent> {

		private static final double BYTES_TO_MEGABYTES_FACTOR = 1.0 / IO_SAMPLING_INTERVAL / 1024.0 / 1024.0;
		private static final double SMOOTHING_FACTOR = 0.3;
		private static final long EFFECTIVELY_ZERO = 100000; // 100kb
		private final Object sampler;
		private final Series<Number, Number> decryptedBytes;
		private final Series<Number, Number> encryptedBytes;
		private final int step = 0;
		private final long oldDecBytes = 0;
		private final long oldEncBytes = 0;

		public IoSamplingAnimationHandler(Object sampler, Series<Number, Number> decryptedBytes, Series<Number, Number> encryptedBytes) {
			this.sampler = sampler;
			this.decryptedBytes = decryptedBytes;
			this.encryptedBytes = encryptedBytes;
		}

		@Override
		public void handle(ActionEvent event) {
			/*
			 * step++;
			 * 
			 * final long decBytes = sampler.pollDecryptedBytes(true);
			 * final double smoothedDecBytes = oldDecBytes + SMOOTHING_FACTOR * (decBytes - oldDecBytes);
			 * final double smoothedDecMb = smoothedDecBytes * BYTES_TO_MEGABYTES_FACTOR;
			 * oldDecBytes = smoothedDecBytes > EFFECTIVELY_ZERO ? (long) smoothedDecBytes : 0l;
			 * decryptedBytes.getData().add(new Data<Number, Number>(step, smoothedDecMb));
			 * if (decryptedBytes.getData().size() > IO_SAMPLING_STEPS) {
			 * decryptedBytes.getData().remove(0);
			 * }
			 * 
			 * final long encBytes = sampler.pollEncryptedBytes(true);
			 * final double smoothedEncBytes = oldEncBytes + SMOOTHING_FACTOR * (encBytes - oldEncBytes);
			 * final double smoothedEncMb = smoothedEncBytes * BYTES_TO_MEGABYTES_FACTOR;
			 * oldEncBytes = smoothedEncBytes > EFFECTIVELY_ZERO ? (long) smoothedEncBytes : 0l;
			 * encryptedBytes.getData().add(new Data<Number, Number>(step, smoothedEncMb));
			 * if (encryptedBytes.getData().size() > IO_SAMPLING_STEPS) {
			 * encryptedBytes.getData().remove(0);
			 * }
			 * 
			 * xAxis.setLowerBound(step - IO_SAMPLING_STEPS);
			 * xAxis.setUpperBound(step);
			 */
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
		macWarningsController.setVault(vault);

		// listen to MAC warnings as long as this vault is unlocked:
		final ListChangeListener<String> macWarningsListener = this::macWarningsDidChange;
		vault.getNamesOfResourcesWithInvalidMac().addListener(macWarningsListener);
		vault.unlockedProperty().addListener((observable, oldValue, newValue) -> {
			if (Boolean.FALSE.equals(newValue)) {
				vault.getNamesOfResourcesWithInvalidMac().removeListener(macWarningsListener);
			}
		});

		// sample crypto-throughput:
		/*
		 * stopIoSampling();
		 * if (vault.getCryptor() instanceof CryptorIOSampling) {
		 * startIoSampling((CryptorIOSampling) vault.getCryptor());
		 * } else {
		 * ioGraph.setVisible(false);
		 * }
		 */
	}

	public LockListener getListener() {
		return listener;
	}

	public void setListener(LockListener listener) {
		this.listener = listener;
	}

	/* callback */

	interface LockListener {
		void didLock(UnlockedController ctrl);
	}

}
