/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - password strength meter
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;

@Singleton
public class ChangePasswordController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Application app;
	final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private Optional<ChangePasswordListener> listener = Optional.empty();
	final IntegerProperty passwordStrength = new SimpleIntegerProperty(); // 0-4

	@Inject
	public ChangePasswordController(Application app, Localization localization) {
		super(localization);
		this.app = app;
	}

	@Inject
	PasswordStrengthUtil strengthRater;

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
	private Rectangle passwordStrengthShape;

	@Override
	public void initialize() {
		BooleanBinding oldPasswordIsEmpty = oldPasswordField.textProperty().isEmpty();
		BooleanBinding newPasswordIsEmpty = newPasswordField.textProperty().isEmpty();
		BooleanBinding passwordsDiffer = newPasswordField.textProperty().isNotEqualTo(retypePasswordField.textProperty());
		changePasswordButton.disableProperty().bind(oldPasswordIsEmpty.or(newPasswordIsEmpty.or(passwordsDiffer)));
		passwordStrength.bind(EasyBind.map(newPasswordField.textProperty(), strengthRater::computeRate));

		passwordStrengthShape.widthProperty().bind(EasyBind.map(passwordStrength, strengthRater::getWidth));
		passwordStrengthShape.fillProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthColor));
		passwordStrengthShape.strokeWidthProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrokeWidth));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/change_password.fxml");
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
			vault.get().changePassphrase(oldPasswordField.getCharacters(), newPasswordField.getCharacters());
			messageText.setText(localization.getString("changePassword.infoMessage.success"));
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
