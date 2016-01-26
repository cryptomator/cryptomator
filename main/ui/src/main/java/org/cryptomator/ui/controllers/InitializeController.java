/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

@Singleton
public class InitializeController extends AbstractFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private Vault vault;
	private InitializationListener listener;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Label messageLabel;

	@Inject
	public InitializeController() {
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/initialize.fxml");
	}

	@Override
	protected ResourceBundle getFxmlResourceBundle() {
		return ResourceBundle.getBundle("localization");
	}

	@Override
	public void initialize() {
		passwordField.textProperty().addListener(this::passwordFieldsDidChange);
		retypePasswordField.textProperty().addListener(this::passwordFieldsDidChange);
	}

	// ****************************************
	// Password fields
	// ****************************************

	private void passwordFieldsDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		boolean passwordIsEmpty = passwordField.getText().isEmpty();
		boolean passwordsAreEqual = passwordField.getText().equals(retypePasswordField.getText());
		okButton.setDisable(passwordIsEmpty || !passwordsAreEqual);
	}

	// ****************************************
	// OK button
	// ****************************************

	@FXML
	protected void initializeVault(ActionEvent event) {
		setControlsDisabled(true);
		final CharSequence passphrase = passwordField.getCharacters();
		try {
			vault.create(passphrase);
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(resourceBundle.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (IOException ex) {
			LOG.error("I/O Exception", ex);
		} finally {
			setControlsDisabled(false);
			passwordField.swipe();
			retypePasswordField.swipe();
		}
	}

	private void setControlsDisabled(boolean disable) {
		passwordField.setDisable(disable);
		retypePasswordField.setDisable(disable);
		okButton.setDisable(disable);
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
	}

	public InitializationListener getListener() {
		return listener;
	}

	public void setListener(InitializationListener listener) {
		this.listener = listener;
	}

	/* callback */

	interface InitializationListener {
		void didInitialize(InitializeController ctrl);
	}

}
