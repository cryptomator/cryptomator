/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

public class ChangePasswordController implements ViewController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Application app;
	private final PasswordStrengthUtil strengthRater;
	private final Localization localization;
	private final IntegerProperty passwordStrength = new SimpleIntegerProperty(-1); // 0-4
	private Optional<ChangePasswordListener> listener = Optional.empty();
	private Vault vault;

	@Inject
	public ChangePasswordController(Application app, PasswordStrengthUtil strengthRater, Localization localization) {
		this.app = app;
		this.strengthRater = strengthRater;
		this.localization = localization;
	}

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
		oldPasswordField.textProperty().addListener(this::passwordsChanged);
		newPasswordField.textProperty().addListener(this::passwordsChanged);
		retypePasswordField.textProperty().addListener(this::passwordsChanged);

		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	private void passwordsChanged(@SuppressWarnings("unused") Observable observable) {
		boolean oldPasswordEmpty = oldPasswordField.getCharacters().length() == 0;
		boolean newPasswordEmpty = newPasswordField.getCharacters().length() == 0;
		boolean passwordsEqual = newPasswordField.getCharacters().equals(retypePasswordField.getCharacters());
		changePasswordButton.setDisable(oldPasswordEmpty || newPasswordEmpty || !passwordsEqual);
		passwordStrength.set(strengthRater.computeRate(newPasswordField.getCharacters().toString()));
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@Override
	public void focus() {
		oldPasswordField.requestFocus();
	}

	void setVault(Vault vault) {
		this.vault = Objects.requireNonNull(vault);
		// trigger "default" change to refresh key bindings:
		changePasswordButton.setDefaultButton(false);
		changePasswordButton.setDefaultButton(true);
		messageText.setText(null);
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
			messageText.setText(null);
			listener.ifPresent(this::invokeListenerLater);
		} catch (InvalidPassphraseException e) {
			messageText.setText(localization.getString("changePassword.errorMessage.wrongPassword"));
			Platform.runLater(oldPasswordField::requestFocus);
		} catch (UncheckedIOException | IOException ex) {
			messageText.setText(localization.getString("changePassword.errorMessage.decryptionFailed"));
			LOG.error("Decryption failed for technical reasons.", ex);
		} catch (UnsupportedVaultFormatException e) {
			downloadsPageLink.setVisible(true);
			LOG.warn("Unable to unlock vault: " + e.getMessage());
			if (e.isVaultOlderThanSoftware()) {
				messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.vaultOlderThanSoftware") + " ");
			} else if (e.isSoftwareOlderThanVault()) {
				messageText.setText(localization.getString("unlock.errorMessage.unsupportedVersion.softwareOlderThanVault") + " ");
			}
		} finally {
			oldPasswordField.swipe();
			newPasswordField.swipe();
			retypePasswordField.swipe();
		}
	}

	/* Getter/Setter */

	public ChangePasswordListener getListener() {
		return listener.orElse(null);
	}

	public void setListener(ChangePasswordListener listener) {
		this.listener = Optional.ofNullable(listener);
	}

	/* callback */

	private void invokeListenerLater(ChangePasswordListener listener) {
		Platform.runLater(() -> {
			listener.didChangePassword();
		});
	}

	@FunctionalInterface
	interface ChangePasswordListener {
		void didChangePassword();
	}

}
