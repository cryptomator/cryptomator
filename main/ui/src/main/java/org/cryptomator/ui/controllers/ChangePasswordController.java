/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;

@Singleton
public class ChangePasswordController extends AbstractFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private Optional<ChangePasswordListener> listener = Optional.empty();
	private Vault vault;

	@FXML
	private SecPasswordField oldPasswordField;

	@FXML
	private SecPasswordField newPasswordField;

	@FXML
	private SecPasswordField retypePasswordField;

	@FXML
	private Button changePasswordButton;

	@FXML
	private Text messageText;

	@FXML
	private Hyperlink downloadsPageLink;

	private final Application app;

	@Inject
	public ChangePasswordController(Application app) {
		super();
		this.app = app;
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/change_password.fxml");
	}

	@Override
	protected ResourceBundle getFxmlResourceBundle() {
		return ResourceBundle.getBundle("localization");
	}

	@Override
	public void initialize() {
		oldPasswordField.textProperty().addListener(this::passwordFieldsDidChange);
		newPasswordField.textProperty().addListener(this::passwordFieldsDidChange);
		retypePasswordField.textProperty().addListener(this::passwordFieldsDidChange);
	}

	// ****************************************
	// Password fields
	// ****************************************

	private void passwordFieldsDidChange(ObservableValue<? extends String> property, String oldValue, String newValue) {
		boolean oldPasswordIsEmpty = oldPasswordField.getText().isEmpty();
		boolean newPasswordIsEmpty = newPasswordField.getText().isEmpty();
		boolean passwordsAreEqual = newPasswordField.getText().equals(retypePasswordField.getText());
		changePasswordButton.setDisable(oldPasswordIsEmpty || newPasswordIsEmpty || !passwordsAreEqual);
	}

	// ****************************************
	// Downloads link
	// ****************************************

	@FXML
	public void didClickDownloadsLink(ActionEvent event) {
		app.getHostServices().showDocument("https://cryptomator.org/downloads/");
	}

	// ****************************************
	// Change password button
	// ****************************************

	@FXML
	private void didClickChangePasswordButton(ActionEvent event) {
		downloadsPageLink.setVisible(false);
		try {
			vault.changePassphrase(oldPasswordField.getCharacters(), newPasswordField.getCharacters());
			messageText.setText(resourceBundle.getString("changePassword.infoMessage.success"));
			listener.ifPresent(this::invokeListenerLater);
		} catch (InvalidPassphraseException e) {
			messageText.setText(resourceBundle.getString("changePassword.errorMessage.wrongPassword"));
			newPasswordField.swipe();
			retypePasswordField.swipe();
			Platform.runLater(oldPasswordField::requestFocus);
			return;
		} catch (IOException ex) {
			messageText.setText(resourceBundle.getString("changePassword.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
			newPasswordField.swipe();
			retypePasswordField.swipe();
			return;
			// } catch (UnsupportedVaultException e) {
			// downloadsPageLink.setVisible(true);
			// if (e.isVaultOlderThanSoftware()) {
			// messageText.setText(resourceBundle.getString("changePassword.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
			// } else if (e.isSoftwareOlderThanVault()) {
			// messageText.setText(resourceBundle.getString("changePassword.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
			// }
			// newPasswordField.swipe();
			// retypePasswordField.swipe();
			// return;
		} finally {
			oldPasswordField.swipe();
			newPasswordField.swipe();
			retypePasswordField.swipe();
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
	}

	public ChangePasswordListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(ChangePasswordListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(ChangePasswordListener listener) {
		Platform.runLater(() -> {
			listener.didChangePassword(this);
		});
	}

	@FunctionalInterface
	interface ChangePasswordListener {
		void didChangePassword(ChangePasswordController ctrl);
	}

}
