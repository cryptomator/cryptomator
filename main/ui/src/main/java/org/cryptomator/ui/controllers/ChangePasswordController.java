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
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
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
	private Zxcvbn zxcvbn = new Zxcvbn();
	private List<String> sanitizedInputs = new ArrayList();

	@Inject
	public ChangePasswordController(Application app, Localization localization) {
		super(localization);
		this.app = app;
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
	private Rectangle passwordStrengthShape;

	@Override
	public void initialize() {
		BooleanBinding oldPasswordIsEmpty = oldPasswordField.textProperty().isEmpty();
		BooleanBinding newPasswordIsEmpty = newPasswordField.textProperty().isEmpty();
		BooleanBinding passwordsDiffer = newPasswordField.textProperty().isNotEqualTo(retypePasswordField.textProperty());
		changePasswordButton.disableProperty().bind(oldPasswordIsEmpty.or(newPasswordIsEmpty.or(passwordsDiffer)));
		newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
			checkPasswordStrength(newValue);
		});

		// default password strength bar visual properties
		passwordStrengthShape.setStroke(Color.GRAY);
		changeProgressBarAspect(0f, 0f, Color.web("#FF0000"));
		passwordStrengthLabel.setText(localization.getString("initialize.messageLabel.passwordStrength") + " : 0%");

		// preparing inputs for the password strength checker
		sanitizedInputs.add("cryptomator");
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

	// ****************************************
	// Password strength management
	// ****************************************

	private void checkPasswordStrength(String password) {
		int strengthPercentage = 0;
		if (StringUtils.isEmpty(password)) {
			changeProgressBarAspect(0f, 0f, Color.web("#FF0000"));
			passwordStrengthLabel.setText(localization.getString("initialize.messageLabel.passwordStrength") + " : " + strengthPercentage + "%");
		} else {
			Color color = Color.web("#FF0000");
			Strength strength = zxcvbn.measure(password, sanitizedInputs);
			switch (strength.getScore()) {
				case 0:
					strengthPercentage = 20;
					break;
				case 1:
					strengthPercentage = 40;
					color = Color.web("#FF8000");
					break;
				case 2:
					strengthPercentage = 60;
					color = Color.web("#FFBF00");
					break;
				case 3:
					strengthPercentage = 80;
					color = Color.web("#FFFF00");
					break;
				case 4:
					strengthPercentage = 100;
					color = Color.web("#BFFF00");
					break;
			}

			passwordStrengthLabel.setText(localization.getString("initialize.messageLabel.passwordStrength") + " : " + strengthPercentage + "%");
			changeProgressBarAspect(0.5f, strengthPercentage * 2.23f, color); // 2.23f is the factor used to get the width to fit the window
		}
	}

	private void changeProgressBarAspect(float strokeWidth, float length, Color color) {
		passwordStrengthShape.setFill(color);
		passwordStrengthShape.setStrokeWidth(strokeWidth);
		passwordStrengthShape.setWidth(length);
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
