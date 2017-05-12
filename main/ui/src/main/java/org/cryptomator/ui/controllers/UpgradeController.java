/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.UpgradeStrategies;
import org.cryptomator.ui.model.UpgradeStrategy;
import org.cryptomator.ui.model.UpgradeStrategy.UpgradeFailedException;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.AsyncTaskService;
import org.fxmisc.easybind.EasyBind;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;

public class UpgradeController implements ViewController {

	private final ObjectProperty<UpgradeStrategy> strategy = new SimpleObjectProperty<>();
	private final UpgradeStrategies strategies;
	private final AsyncTaskService asyncTaskService;
	private Optional<UpgradeListener> listener = Optional.empty();
	private Vault vault;

	@Inject
	public UpgradeController(Localization localization, UpgradeStrategies strategies, AsyncTaskService asyncTaskService) {
		this.strategies = strategies;
		this.asyncTaskService = asyncTaskService;
	}

	@FXML
	private Label upgradeTitleLabel;

	@FXML
	private Label upgradeMsgLabel;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private CheckBox confirmationCheckbox;

	@FXML
	private Button upgradeButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label errorLabel;

	@FXML
	private GridPane root;

	@Override
	public void initialize() {
		upgradeTitleLabel.textProperty().bind(EasyBind.monadic(strategy).map(this::upgradeTitle).orElse(""));
		upgradeMsgLabel.textProperty().bind(EasyBind.monadic(strategy).map(this::upgradeMessage).orElse(""));

		BooleanExpression passwordProvided = passwordField.textProperty().isNotEmpty().and(passwordField.disabledProperty().not());
		BooleanExpression syncFinished = confirmationCheckbox.selectedProperty();
		upgradeButton.disableProperty().bind(passwordProvided.not().or(syncFinished.not()));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		errorLabel.setText(null);
		strategy.set(strategies.getUpgradeStrategy(vault));
		// trigger "default" change to refresh key bindings:
		upgradeButton.setDefaultButton(false);
		upgradeButton.setDefaultButton(true);
	}

	// ****************************************
	// Upgrade label
	// ****************************************

	private String upgradeTitle(UpgradeStrategy instruction) {
		return instruction.getTitle(vault);
	}

	private String upgradeMessage(UpgradeStrategy instruction) {
		return instruction.getMessage(vault);
	}

	// ****************************************
	// Upgrade button
	// ****************************************

	@FXML
	private void didClickUpgradeButton(ActionEvent event) {
		EasyBind.monadic(strategy).ifPresent(this::upgrade);
	}

	private void upgrade(UpgradeStrategy instruction) {
		passwordField.setDisable(true);
		progressIndicator.setVisible(true);
		asyncTaskService //
				.asyncTaskOf(() -> {
					if (!instruction.isApplicable(vault)) {
						throw new IllegalStateException("No ugprade needed for " + vault.getPath());
					}
					instruction.upgrade(vault, passwordField.getCharacters());
				}) //
				.onSuccess(this::showNextUpgrade) //
				.onError(UpgradeFailedException.class, e -> {
					errorLabel.setText(e.getLocalizedMessage());
				}) //
				.andFinally(() -> {
					progressIndicator.setVisible(false);
					passwordField.setDisable(false);
					passwordField.swipe();
				}).run();
	}

	private void showNextUpgrade() {
		errorLabel.setText(null);
		UpgradeStrategy nextStrategy = strategies.getUpgradeStrategy(vault);
		if (nextStrategy != null) {
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
