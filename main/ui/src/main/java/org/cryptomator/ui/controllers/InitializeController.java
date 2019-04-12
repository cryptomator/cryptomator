/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;
import java.util.Optional;

public class InitializeController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private final Localization localization;
	private final PasswordStrengthUtil strengthRater;
	private IntegerProperty passwordStrength = new SimpleIntegerProperty(-1); // strengths: 0-4
	private Optional<InitializationListener> listener = Optional.empty();
	private Vault vault;

	@Inject
	public InitializeController(Localization localization, PasswordStrengthUtil strengthRater) {
		this.localization = localization;
		this.strengthRater = strengthRater;
	}

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Label messageLabel;

	@FXML
	private Label passwordStrengthLabel;

	@FXML
	private Region passwordStrengthLevel0;

	@FXML
	private Region passwordStrengthLevel1;

	@FXML
	private Region passwordStrengthLevel2;

	@FXML
	private Region passwordStrengthLevel3;

	@FXML
	private Region passwordStrengthLevel4;

	@FXML
	private GridPane root;

	@Override
	public void initialize() {
		passwordField.textProperty().addListener(this::passwordsChanged);
		retypePasswordField.textProperty().addListener(this::passwordsChanged);

		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	private void passwordsChanged(@SuppressWarnings("unused") Observable observable) {
		boolean passwordsEmpty = passwordField.getCharacters().length() == 0;
		boolean passwordsEqual = passwordField.getCharacters().equals(retypePasswordField.getCharacters());
		okButton.setDisable(passwordsEmpty || !passwordsEqual);
		passwordStrength.set(strengthRater.computeRate(passwordField.getCharacters().toString()));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@Override
	public void focus() {
		passwordField.requestFocus();
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		// trigger "default" change to refresh key bindings:
		okButton.setDefaultButton(false);
		okButton.setDefaultButton(true);
	}

	// ****************************************
	// OK button
	// ****************************************

	@FXML
	protected void initializeVault(ActionEvent event) {
		final CharSequence passphrase = passwordField.getCharacters();
		try {
			vault.create(passphrase);
			listener.ifPresent(this::invokeListenerLater);
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
			messageLabel.setText(localization.getString("initialize.messageLabel.initializationFailed"));
		} finally {
			passwordField.swipe();
			retypePasswordField.swipe();
		}
	}

	/* Getter/Setter */

	public InitializationListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(InitializationListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(InitializationListener listener) {
		Platform.runLater(() -> {
			listener.didInitialize();
		});
	}

	@FunctionalInterface
	interface InitializationListener {
		void didInitialize();
	}

}
