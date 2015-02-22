/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	private ResourceBundle localization;
	private Vault directory;
	private InitializationListener listener;

	@FXML
	private SecPasswordField passwordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Label messageLabel;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.localization = rb;
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
		final Path masterKeyPath = directory.getPath().resolve(Vault.VAULT_MASTERKEY_FILE);
		final CharSequence password = passwordField.getCharacters();
		try (OutputStream masterKeyOutputStream = Files.newOutputStream(masterKeyPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			directory.getCryptor().encryptMasterKey(masterKeyOutputStream, password);
			if (listener != null) {
				listener.didInitialize(this);
			}
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (InvalidPathException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.invalidPath"));
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

	public Vault getDirectory() {
		return directory;
	}

	public void setDirectory(Vault directory) {
		this.directory = directory;
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
