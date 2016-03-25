/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;

import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.fxmisc.easybind.EasyBind;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Stage;
import javafx.util.Duration;

public class UnlockedController extends LocalizedFXMLViewController {

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.25;

	private final Stage macWarningsWindow = new Stage();
	private final MacWarningsController macWarningsController;
	private final ExecutorService exec;
	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private Optional<LockListener> listener = Optional.empty();
	private Timeline ioAnimation;

	@Inject
	public UnlockedController(Localization localization, Provider<MacWarningsController> macWarningsControllerProvider, ExecutorService exec) {
		super(localization);
		this.macWarningsController = macWarningsControllerProvider.get();
		this.exec = exec;

		macWarningsController.vault.bind(this.vault);
	}

	@FXML
	private Label messageLabel;

	@FXML
	private LineChart<Number, Number> ioGraph;

	@FXML
	private NumberAxis xAxis;

	@FXML
	private ToggleButton moreOptionsButton;

	@FXML
	private ContextMenu moreOptionsMenu;

	@Override
	public void initialize() {
		macWarningsController.initStage(macWarningsWindow);
		ActiveWindowStyleSupport.startObservingFocus(macWarningsWindow);

		EasyBind.subscribe(vault, this::vaultChanged);
		EasyBind.subscribe(moreOptionsMenu.showingProperty(), moreOptionsButton::setSelected);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/unlocked.fxml");
	}

	private void vaultChanged(Vault newVault) {
		if (newVault == null) {
			return;
		}

		// listen to MAC warnings as long as this vault is unlocked:
		final ListChangeListener<String> macWarningsListener = this::macWarningsDidChange;
		newVault.getNamesOfResourcesWithInvalidMac().addListener(macWarningsListener);
		newVault.unlockedProperty().addListener((observable, oldValue, newValue) -> {
			if (Boolean.FALSE.equals(newValue)) {
				newVault.getNamesOfResourcesWithInvalidMac().removeListener(macWarningsListener);
			}
		});

		// (re)start throughput statistics:
		stopIoSampling();
		startIoSampling();
	}

	@FXML
	private void didClickLockVault(ActionEvent event) {
		exec.submit(() -> {
			try {
				vault.get().unmount();
			} catch (CommandFailedException e) {
				Platform.runLater(() -> {
					messageLabel.setText(localization.getString("unlocked.label.unmountFailed"));
				});
				return;
			}
			vault.get().deactivateFrontend();
			listener.ifPresent(this::invokeListenerLater);
		});
	}

	@FXML
	private void didClickMoreOptions(ActionEvent event) {
		if (moreOptionsMenu.isShowing()) {
			moreOptionsMenu.hide();
		} else {
			moreOptionsMenu.setAnchorLocation(AnchorLocation.CONTENT_TOP_RIGHT);
			moreOptionsMenu.show(moreOptionsButton, Side.BOTTOM, moreOptionsButton.getWidth(), 0.0);
		}
	}

	@FXML
	private void didClickRevealVault(ActionEvent event) {
		exec.submit(() -> {
			try {
				vault.get().reveal();
			} catch (CommandFailedException e) {
				Platform.runLater(() -> {
					messageLabel.setText(localization.getString("unlocked.label.revealFailed"));
				});
			}
		});
	}

	@FXML
	private void didClickCopyUrl(ActionEvent event) {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putUrl(vault.get().getWebDavUrl());
		clipboardContent.putString(vault.get().getWebDavUrl());
		Clipboard.getSystemClipboard().setContent(clipboardContent);
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

	private void startIoSampling() {
		final Series<Number, Number> decryptedBytes = new Series<>();
		decryptedBytes.setName(localization.getString("unlocked.label.statsDecrypted"));
		final Series<Number, Number> encryptedBytes = new Series<>();
		encryptedBytes.setName(localization.getString("unlocked.label.statsEncrypted"));

		ioGraph.getData().add(decryptedBytes);
		ioGraph.getData().add(encryptedBytes);

		ioAnimation = new Timeline();
		ioAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(IO_SAMPLING_INTERVAL), new IoSamplingAnimationHandler(decryptedBytes, encryptedBytes)));
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
		private final Series<Number, Number> decryptedBytes;
		private final Series<Number, Number> encryptedBytes;
		private int step = 0;
		private long oldDecBytes = 0;
		private long oldEncBytes = 0;

		public IoSamplingAnimationHandler(Series<Number, Number> decryptedBytes, Series<Number, Number> encryptedBytes) {
			this.decryptedBytes = decryptedBytes;
			this.encryptedBytes = encryptedBytes;
		}

		@Override
		public void handle(ActionEvent event) {
			step++;

			final long decBytes = vault.get().pollBytesRead();
			final double smoothedDecBytes = oldDecBytes + SMOOTHING_FACTOR * (decBytes - oldDecBytes);
			final double smoothedDecMb = smoothedDecBytes * BYTES_TO_MEGABYTES_FACTOR;
			oldDecBytes = smoothedDecBytes > EFFECTIVELY_ZERO ? (long) smoothedDecBytes : 0l;
			decryptedBytes.getData().add(new Data<Number, Number>(step, smoothedDecMb));
			if (decryptedBytes.getData().size() > IO_SAMPLING_STEPS) {
				decryptedBytes.getData().remove(0);
			}

			final long encBytes = vault.get().pollBytesWritten();
			final double smoothedEncBytes = oldEncBytes + SMOOTHING_FACTOR * (encBytes - oldEncBytes);
			final double smoothedEncMb = smoothedEncBytes * BYTES_TO_MEGABYTES_FACTOR;
			oldEncBytes = smoothedEncBytes > EFFECTIVELY_ZERO ? (long) smoothedEncBytes : 0l;
			encryptedBytes.getData().add(new Data<Number, Number>(step, smoothedEncMb));
			if (encryptedBytes.getData().size() > IO_SAMPLING_STEPS) {
				encryptedBytes.getData().remove(0);
			}

			xAxis.setLowerBound(step - IO_SAMPLING_STEPS);
			xAxis.setUpperBound(step);
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return this.vault.get();
	}

	public void setVault(Vault vault) {
		this.vault.set(vault);
	}

	/* callback */

	public void setListener(LockListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	private void invokeListenerLater(LockListener listener) {
		Platform.runLater(() -> {
			listener.didLock(this);
		});
	}

	@FunctionalInterface
	interface LockListener {
		void didLock(UnlockedController ctrl);
	}

}
