/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.AsyncTaskService;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Runnables;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.ObjectExpression;
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
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;

public class UnlockedController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockedController.class);

	private static final int IO_SAMPLING_STEPS = 100;
	private static final double IO_SAMPLING_INTERVAL = 0.25;

	private final Localization localization;
	private final AsyncTaskService asyncTaskService;
	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final ObjectExpression<Vault.State> vaultState = ObjectExpression.objectExpression(EasyBind.select(vault).selectObject(Vault::stateProperty));
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
	private MenuItem mountVaultMenuItem;

	@FXML
	private MenuItem unmountVaultMenuItem;

	@FXML
	private MenuItem revealVaultMenuItem;

	@FXML
	private VBox root;

	@Inject
	public UnlockedController(Localization localization, AsyncTaskService asyncTaskService) {
		this.localization = localization;
		this.asyncTaskService = asyncTaskService;
	}

	@Override
	public void initialize() {
		mountVaultMenuItem.disableProperty().bind(vaultState.isEqualTo(Vault.State.UNLOCKED).not()); // enable when unlocked
		unmountVaultMenuItem.disableProperty().bind(vaultState.isEqualTo(Vault.State.MOUNTED).not()); // enable when mounted
		revealVaultMenuItem.disableProperty().bind(vaultState.isEqualTo(Vault.State.MOUNTED).not()); // enable when mounted

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

		if (newVault.getState() == Vault.State.UNLOCKED && newVault.getVaultSettings().mountAfterUnlock().get()) {
			mountVault(newVault);
		}

		// (re)start throughput statistics:
		stopIoSampling();
		startIoSampling();
	}

	@FXML
	private void didClickLockVault(ActionEvent event) {
		regularUnmountVault(this::lockVault);
	}

	private void lockVault() {
		try {
			vault.get().lock();
		} catch (ServerLifecycleException | IOException e) {
			LOG.error("Lock failed", e);
		}
		listener.ifPresent(listener -> listener.didLock(this));
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
	public void didClickMountVault(ActionEvent event) {
		mountVault(vault.get());
	}

	private void mountVault(Vault vault) {
		asyncTaskService.asyncTaskOf(() -> {
			vault.mount();
		}).onSuccess(() -> {
			LOG.trace("Mount succeeded.");
			messageLabel.setText(null);
			if (vault.getVaultSettings().revealAfterMount().get()) {
				revealVault(vault);
			}
		}).onError(CommandFailedException.class, e -> {
			LOG.error("Mount failed.", e);
			// TODO Markus Kreusch #393: hyperlink auf FAQ oder sowas?
			messageLabel.setText(localization.getString("unlocked.label.mountFailed"));
		}).run();
	}

	@FXML
	public void didClickUnmountVault(ActionEvent event) {
		regularUnmountVault(Runnables.doNothing());
	}

	private void regularUnmountVault(Runnable onSuccess) {
		asyncTaskService.asyncTaskOf(() -> {
			vault.get().unmount();
		}).onSuccess(() -> {
			LOG.trace("Regular unmount succeeded.");
			onSuccess.run();
		}).onError(Exception.class, e -> {
			onRegularUnmountVaultFailed(e, onSuccess);
		}).run();
	}

	private void forcedUnmountVault(Runnable onSuccess) {
		asyncTaskService.asyncTaskOf(() -> {
			vault.get().unmountForced();
		}).onSuccess(() -> {
			LOG.trace("Forced unmount succeeded.");
			onSuccess.run();
		}).onError(Exception.class, e -> {
			LOG.error("Forced unmount failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.unmountFailed"));
		}).run();
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
				forcedUnmountVault(onSuccess);
			} else {
				LOG.trace("Unmount cancelled.", e);
			}
		} else {
			LOG.error("Regular unmount failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.unmountFailed"));
		}
	}

	@FXML
	private void didClickRevealVault(ActionEvent event) {
		revealVault(vault.get());
	}

	private void revealVault(Vault vault) {
		asyncTaskService.asyncTaskOf(() -> {
			vault.reveal();
		}).onSuccess(() -> {
			LOG.trace("Reveal succeeded.");
			messageLabel.setText(null);
		}).onError(CommandFailedException.class, e -> {
			LOG.error("Reveal failed.", e);
			messageLabel.setText(localization.getString("unlocked.label.revealFailed"));
		}).run();
	}

	@FXML
	private void didClickCopyUrl(ActionEvent event) {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putUrl(vault.get().getWebDavUrl());
		clipboardContent.putString(vault.get().getWebDavUrl());
		Clipboard.getSystemClipboard().setContent(clipboardContent);
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

	@FunctionalInterface
	interface LockListener {
		void didLock(UnlockedController ctrl);
	}

}
