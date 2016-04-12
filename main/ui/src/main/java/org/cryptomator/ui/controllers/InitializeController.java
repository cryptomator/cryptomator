/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nulabinc.zxcvbn.*;

@Singleton
public class InitializeController extends LocalizedFXMLViewController {

	private static final Logger LOG = LoggerFactory.getLogger(InitializeController.class);

	final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private Optional<InitializationListener> listener = Optional.empty();
	private Zxcvbn zxcvbn = new Zxcvbn();
	private List<String> sanitizedInputs = new ArrayList();

	@Inject
	public InitializeController(Localization localization) {
		super(localization);
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
	private Rectangle passwordStrengthShape;

	@Override
	public void initialize() {
		BooleanBinding passwordIsEmpty = passwordField.textProperty().isEmpty();
		BooleanBinding passwordsDiffer = passwordField.textProperty().isNotEqualTo(retypePasswordField.textProperty());
		okButton.disableProperty().bind(passwordIsEmpty.or(passwordsDiffer));
		passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
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
		return getClass().getResource("/fxml/initialize.fxml");
	}

	// ****************************************
	// OK button
	// ****************************************

	@FXML
	protected void initializeVault(ActionEvent event) {
		final CharSequence passphrase = passwordField.getCharacters();
		try {
			vault.get().create(passphrase);
			listener.ifPresent(this::invokeListenerLater);
		} catch (FileAlreadyExistsException ex) {
			messageLabel.setText(localization.getString("initialize.messageLabel.alreadyInitialized"));
		} catch (UncheckedIOException | IOException ex) {
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

	/* Methods */

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
