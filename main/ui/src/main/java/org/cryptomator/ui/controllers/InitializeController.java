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

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
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
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
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
	final IntegerProperty passwordStrength = new SimpleIntegerProperty(); // 0-4

	@Inject
	public InitializeController(Localization localization) {
		super(localization);
	}

	@Inject
	PasswordStrengthUtil strengthRater;

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
		EasyBind.subscribe(passwordField.textProperty(), this::checkPasswordStrength);

		strengthRater.setLocalization(localization);

		passwordStrengthShape.widthProperty().bind(EasyBind.map(passwordStrength, strengthRater::getWidth));
		passwordStrengthShape.fillProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthColor));
		passwordStrengthShape.strokeWidthProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrokeWidth));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
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
		passwordStrength.set(strengthRater.computeRate(password));
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
