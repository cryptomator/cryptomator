/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.l10n.Localization;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

@Singleton
public class SettingsController implements ViewController {

	private static final CharMatcher DIGITS_MATCHER = CharMatcher.inRange('0', '9');

	private final Localization localization;
	private final Settings settings;
	private final Optional<String> applicationVersion;

	@Inject
	public SettingsController(Localization localization, Settings settings, @Named("applicationVersion") Optional<String> applicationVersion) {
		this.localization = localization;
		this.settings = settings;
		this.applicationVersion = applicationVersion;
	}

	@FXML
	private CheckBox checkForUpdatesCheckbox;

	@FXML
	private TextField portField;

	@FXML
	private Button changePortButton;

	@FXML
	private Label versionLabel;

	@FXML
	private Label prefGvfsSchemeLabel;

	@FXML
	private ChoiceBox<String> prefGvfsScheme;

	@FXML
	private CheckBox debugModeCheckbox;

	@FXML
	private VBox root;

	@Override
	public void initialize() {
		checkForUpdatesCheckbox.setDisable(areUpdatesManagedExternally());
		checkForUpdatesCheckbox.setSelected(settings.checkForUpdates().get() && !areUpdatesManagedExternally());
		portField.setText(String.valueOf(settings.port().intValue()));
		portField.addEventFilter(KeyEvent.KEY_TYPED, this::filterNumericKeyEvents);
		changePortButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(portField.textProperty()));
		changePortButton.disableProperty().bind(Bindings.createBooleanBinding(this::isPortValid, portField.textProperty()).not());
		versionLabel.setText(String.format(localization.getString("settings.version.label"), applicationVersion.orElse("SNAPSHOT")));
		prefGvfsSchemeLabel.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.getItems().add("dav");
		prefGvfsScheme.getItems().add("webdav");
		prefGvfsScheme.setValue(settings.preferredGvfsScheme().get());
		debugModeCheckbox.setSelected(settings.debugMode().get());

		settings.checkForUpdates().bind(checkForUpdatesCheckbox.selectedProperty());
		settings.preferredGvfsScheme().bind(prefGvfsScheme.valueProperty());
		settings.debugMode().bind(debugModeCheckbox.selectedProperty());
	}

	@Override
	public Parent getRoot() {
		return root;
	}

	@FXML
	private void changePort(ActionEvent evt) {
		assert isPortValid() : "Button must be disabled, if port is invalid.";
		try {
			int port = Integer.parseInt(portField.getText());
			settings.port().set(port);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Button must be disabled, if port is invalid.", e);
		}
	}

	private boolean isPortValid() {
		try {
			int port = Integer.parseInt(portField.getText());
			if (port == 0 || port >= Settings.MIN_PORT && port <= Settings.MAX_PORT) {
				return true;
			} else {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void filterNumericKeyEvents(KeyEvent t) {
		if (!Strings.isNullOrEmpty(t.getCharacter()) && !DIGITS_MATCHER.matchesAllOf(t.getCharacter())) {
			t.consume();
		}
	}

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

}
