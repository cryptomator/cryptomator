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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.controls.ClearOnDisableListener;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);
	private static final int MAX_USERNAME_LENGTH = 250;

	private ResourceBundle localization;
	private Vault directory;
	private InitializationListener listener;

	@FXML
	private TextField usernameField;

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
		usernameField.addEventFilter(KeyEvent.KEY_TYPED, this::filterAlphanumericKeyEvents);
		usernameField.textProperty().addListener(this::usernameFieldDidChange);
		passwordField.textProperty().addListener(this::passwordFieldDidChange);
		retypePasswordField.textProperty().addListener(this::retypePasswordFieldDidChange);
		retypePasswordField.disableProperty().addListener(new ClearOnDisableListener(retypePasswordField));

	}

	// ****************************************
	// Username field
	// ****************************************

	public void filterAlphanumericKeyEvents(KeyEvent t) {
		if (t.getCharacter() == null || t.getCharacter().length() == 0) {
			return;
		}
		char c = t.getCharacter().charAt(0);
		if (!CharUtils.isAsciiAlphanumeric(c)) {
			t.consume();
		}
	}

	public void usernameFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		if (StringUtils.length(newValue) > MAX_USERNAME_LENGTH) {
			usernameField.setText(newValue.substring(0, MAX_USERNAME_LENGTH));
		}
		passwordField.setDisable(StringUtils.isEmpty(newValue));
	}

	// ****************************************
	// Password field
	// ****************************************

	private void passwordFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		retypePasswordField.setDisable(StringUtils.isEmpty(newValue));
	}

	// ****************************************
	// Retype password field
	// ****************************************

	private void retypePasswordFieldDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		boolean passwordsAreEqual = passwordField.getText().equals(retypePasswordField.getText());
		okButton.setDisable(!passwordsAreEqual);
	}

	// ****************************************
	// OK button
	// ****************************************

	@FXML
	protected void initializeVault(ActionEvent event) {
		setControlsDisabled(true);
		final String masterKeyFileName = usernameField.getText() + Aes256Cryptor.MASTERKEY_FILE_EXT;
		final Path masterKeyPath = directory.getPath().resolve(masterKeyFileName);
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
			usernameField.setText(null);
			passwordField.swipe();
			retypePasswordField.swipe();
		}
	}

	private void setControlsDisabled(boolean disable) {
		usernameField.setDisable(disable);
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
