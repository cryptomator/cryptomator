package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.UpgradeStrategies;
import org.cryptomator.ui.model.UpgradeStrategy;
import org.cryptomator.ui.model.UpgradeStrategy.UpgradeFailedException;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
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
	final ObjectProperty<Optional<UpgradeStrategy>> strategy = new SimpleObjectProperty<>();
	private final UpgradeStrategies strategies;
	private final ExecutorService exec;
	private Optional<UpgradeListener> listener = Optional.empty();

	@Inject
	public UpgradeController(Localization localization, UpgradeStrategies strategies, ExecutorService exec) {
		super(localization);
		this.strategies = strategies;
		this.exec = exec;
	}

	@FXML
	private Label upgradeLabel;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private Button upgradeButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label errorLabel;

	@Override
	protected void initialize() {
		upgradeLabel.textProperty().bind(EasyBind.monadic(strategy).map(instruction -> {
			return instruction.map(this::upgradeNotification).orElse("");
		}).orElse(""));

		upgradeButton.disableProperty().bind(passwordField.textProperty().isEmpty().or(passwordField.disabledProperty()));

		EasyBind.subscribe(vault, this::vaultDidChange);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/upgrade.fxml");
	}

	private void vaultDidChange(Vault newVault) {
		errorLabel.setText(null);
		strategy.set(strategies.getUpgradeStrategy(newVault));
	}

	// ****************************************
	// Upgrade label
	// ****************************************

	private String upgradeNotification(UpgradeStrategy instruction) {
		return instruction.getNotification(vault.get());
	}

	// ****************************************
	// Upgrade button
	// ****************************************

	@FXML
	private void didClickUpgradeButton(ActionEvent event) {
		strategy.getValue().ifPresent(this::upgrade);
	}

	private void upgrade(UpgradeStrategy instruction) {
		Vault v = Objects.requireNonNull(vault.getValue());
		passwordField.setDisable(true);
		progressIndicator.setVisible(true);
		exec.submit(() -> {
			if (!instruction.isApplicable(v)) {
				LOG.error("No upgrade needed for " + v.path().getValue());
				throw new IllegalStateException("No ugprade needed for " + v.path().getValue());
			}
			try {
				instruction.upgrade(v, passwordField.getCharacters());
				Platform.runLater(this::showNextUpgrade);
			} catch (UpgradeFailedException e) {
				Platform.runLater(() -> {
					errorLabel.setText(e.getLocalizedMessage());
				});
			} finally {
				Platform.runLater(() -> {
					progressIndicator.setVisible(false);
					passwordField.setDisable(false);
					passwordField.swipe();
				});
			}
		});
	}

	private void showNextUpgrade() {
		errorLabel.setText(null);
		Optional<UpgradeStrategy> nextStrategy = strategies.getUpgradeStrategy(vault.getValue());
		if (nextStrategy.isPresent()) {
			strategy.set(nextStrategy);
		} else {
			listener.ifPresent(UpgradeListener::didUpgrade);
		}
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
