package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.cryptomator.ui.model.UpgradeInstruction;
import org.cryptomator.ui.model.UpgradeInstruction.UpgradeFailedException;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

public class UpgradeController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeController.class);

	final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final ExecutorService exec;
	private final Binding<Optional<UpgradeInstruction>> upgradeInstruction = EasyBind.monadic(vault).map(Vault::availableUpgrade);
	private Optional<UpgradeListener> listener = Optional.empty();

	@Inject
	public UpgradeController(Localization localization, ExecutorService exec) {
		super(localization);
		this.exec = exec;
	}

	@FXML
	private Label upgradeLabel;

	@FXML
	private Button upgradeButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label errorLabel;

	@Override
	protected void initialize() {
		upgradeLabel.textProperty().bind(EasyBind.monadic(upgradeInstruction).map(instruction -> {
			return instruction.map(this::upgradeNotification).orElse("");
		}).orElse(""));

		EasyBind.subscribe(vault, this::vaultChanged);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/upgrade.fxml");
	}

	private void vaultChanged(Vault newVault) {
		errorLabel.setText(null);
	}

	// ****************************************
	// Upgrade label
	// ****************************************

	private String upgradeNotification(UpgradeInstruction instruction) {
		return instruction.getNotification(vault.get(), localization);
	}

	// ****************************************
	// Upgrade button
	// ****************************************

	@FXML
	private void didClickUpgradeButton(ActionEvent event) {
		upgradeInstruction.getValue().ifPresent(this::upgrade);
	}

	private void upgrade(UpgradeInstruction instruction) {
		Vault v = vault.getValue();
		Objects.requireNonNull(v);
		progressIndicator.setVisible(true);
		upgradeButton.setDisable(true);
		exec.submit(() -> {
			if (!instruction.isApplicable(v)) {
				LOG.error("No upgrade needed for " + v.path().getValue());
				throw new IllegalStateException("No ugprade needed for " + v.path().getValue());
			}
			try {
				instruction.upgrade(v, localization);
				Platform.runLater(() -> {
					progressIndicator.setVisible(false);
					upgradeButton.setDisable(false);
					listener.ifPresent(UpgradeListener::didUpgrade);
				});
			} catch (UpgradeFailedException e) {
				Platform.runLater(() -> {
					errorLabel.setText(e.getLocalizedMessage());
					progressIndicator.setVisible(false);
					upgradeButton.setDisable(false);
				});
			}
		});
	}

	/* callback */

	public void setListener(UpgradeListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	@FunctionalInterface
	interface UpgradeListener {
		void didUpgrade();
	}

}
