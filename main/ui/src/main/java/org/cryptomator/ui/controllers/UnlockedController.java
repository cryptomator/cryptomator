/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.cryptomator.ui.util.Tasks;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;

public class UnlockedController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockedController.class);

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.5;

	private final Localization localization;
	private final ExecutorService executor;
	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private Optional<LockListener> listener = Optional.empty();
	private Timeline ioAnimation;

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

	@FXML
	private VBox root;

	@Inject
	public UnlockedController(Localization localization, ExecutorService executor) {
		this.localization = localization;
		this.executor = executor;
	}

	@Override
	public void initialize() {
		EasyBind.subscribe(vault, this::vaultChanged);
		EasyBind.subscribe(moreOptionsMenu.showingProperty(), moreOptionsButton::setSelected);
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	private void vaultChanged(Vault newVault) {
		if (newVault == null) {
			return;
		}

		// (re)start throughput statistics:
		stopIoSampling();
		startIoSampling();
	}

	@FXML
	private void didClickLockVault() {
		regularLockVault(this::lockVaultSucceeded);
	}

	private void lockVaultSucceeded() {
		listener.ifPresent(listener -> listener.didLock(this));
	}

	private void regularLockVault(Runnable onSuccess) {
		Tasks.create(() -> {
			vault.get().lock(false);
		}).onSuccess(() -> {
			LOG.trace("Regular unmount succeeded.");
			onSuccess.run();
		}).onError(Exception.class, e -> {
			onRegularUnmountVaultFailed(e, onSuccess);
		}).runOnce(executor);
	}

	private void onRegularUnmountVaultFailed(Exception e, Runnable onSuccess) {
		if (vault.get().supportsForcedUnmount()) {
			LOG.trace("Regular unmount failed.", e);
			Alert confirmDialog = DialogBuilderUtil.buildYesNoDialog( //
					format(localization.getString("unlocked.lock.force.confirmation.title"), vault.get().name().getValue()), //
					localization.getString("unlocked.lock.force.confirmation.header"), //
					localization.getString("unlocked.lock.force.confirmation.content"), //
					ButtonType.NO);

			Optional<ButtonType> choice = confirmDialog.showAndWait();
			if (ButtonType.YES.equals(choice.get())) {
				forcedLockVault(onSuccess);
			} else {
				LOG.trace("Unmount cancelled.", e);
			}
		} else {
			LOG.error("Regular unmount failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.unmountFailed"));
		}
	}

	private void forcedLockVault(Runnable onSuccess) {
		Tasks.create(() -> {
			vault.get().lock(true);
		}).onSuccess(() -> {
			LOG.trace("Forced unmount succeeded.");
			onSuccess.run();
		}).onError(Exception.class, e -> {
			LOG.error("Forced unmount failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.unmountFailed"));
		}).runOnce(executor);
	}

	@FXML
	private void didClickMoreOptions() {
		if (moreOptionsMenu.isShowing()) {
			moreOptionsMenu.hide();
		} else {
			moreOptionsMenu.setAnchorLocation(AnchorLocation.CONTENT_TOP_RIGHT);
			moreOptionsMenu.show(moreOptionsButton, Side.BOTTOM, moreOptionsButton.getWidth(), 0.0);
		}
	}

	@FXML
	private void didClickRevealVault() {
		revealVault(vault.get());
	}

	void revealVault(Vault vault) {
		Tasks.create(() -> {
			vault.reveal();
		}).onSuccess(() -> {
			LOG.trace("Reveal succeeded.");
			messageLabel.setText(null);
		}).onError(Exception.class, e -> {
			LOG.error("Reveal failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.revealFailed"));
		}).runOnce(executor);
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
		private final Series<Number, Number> decryptedBytes;
		private final Series<Number, Number> encryptedBytes;

		public IoSamplingAnimationHandler(Series<Number, Number> decryptedBytes, Series<Number, Number> encryptedBytes) {
			this.decryptedBytes = decryptedBytes;
			this.encryptedBytes = encryptedBytes;

			// initialize data once and change value of datapoints later:
			for (int i = 0; i < IO_SAMPLING_STEPS; i++) {
				decryptedBytes.getData().add(new Data<>(i, 0));
				encryptedBytes.getData().add(new Data<>(i, 0));
			}

			xAxis.setLowerBound(0);
			xAxis.setUpperBound(IO_SAMPLING_STEPS);
		}

		@Override
		public void handle(ActionEvent event) {
			// move all values one step:
			for (int i = 0; i < IO_SAMPLING_STEPS - 1; i++) {
				int j = i + 1;
				Number tmp = decryptedBytes.getData().get(j).getYValue();
				decryptedBytes.getData().get(i).setYValue(tmp);

				tmp = encryptedBytes.getData().get(j).getYValue();
				encryptedBytes.getData().get(i).setYValue(tmp);
			}

			// add latest value:
			final long decBytes = vault.get().pollBytesRead();
			final double decMb = decBytes * BYTES_TO_MEGABYTES_FACTOR;
			final long encBytes = vault.get().pollBytesWritten();
			final double encMb = encBytes * BYTES_TO_MEGABYTES_FACTOR;
			decryptedBytes.getData().get(IO_SAMPLING_STEPS - 1).setYValue(decMb);
			encryptedBytes.getData().get(IO_SAMPLING_STEPS - 1).setYValue(encMb);
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

	@FunctionalInterface
	interface LockListener {

		void didLock(UnlockedController ctrl);
	}

}
