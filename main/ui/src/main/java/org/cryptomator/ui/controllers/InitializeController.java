/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

@Singleton
public class InitializeController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private final PasswordStrengthUtil strengthRater;
	private final IntegerProperty passwordStrength = new SimpleIntegerProperty(); // 0-4
	private Optional<InitializationListener> listener = Optional.empty();
	private Vault vault;

	@Inject
	public InitializeController(Localization localization, PasswordStrengthUtil strengthRater) {
		super(localization);
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

	@Override
	public void initialize() {
		BooleanBinding passwordIsEmpty = passwordField.textProperty().isEmpty();
		BooleanBinding passwordsDiffer = passwordField.textProperty().isNotEqualTo(retypePasswordField.textProperty());
		okButton.disableProperty().bind(passwordIsEmpty.or(passwordsDiffer));
		passwordStrength.bind(EasyBind.map(passwordField.textProperty(), strengthRater::computeRate));

		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/initialize.fxml");
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		passwordField.swipe();
		retypePasswordField.swipe();
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
		} catch (DirectoryNotEmptyException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.notEmpty"));
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
