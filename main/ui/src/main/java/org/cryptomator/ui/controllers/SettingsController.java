/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

@Singleton
public class SettingsController extends LocalizedFXMLViewController {

	private final Settings settings;

	@Inject
	public SettingsController(Localization localization, Settings settings) {
		super(localization);
		this.settings = settings;
	}

	@FXML
	private CheckBox checkForUpdatesCheckbox;

	@FXML
	private TextField portField;

	@FXML
	private Button changePortButton;

	@FXML
	private Label useIpv6Label;

	@FXML
	private CheckBox useIpv6Checkbox;

	@FXML
	private Label versionLabel;

	@FXML
	private Label prefGvfsSchemeLabel;

	@FXML
	private ChoiceBox<String> prefGvfsScheme;

	@FXML
	private CheckBox debugModeCheckbox;

	@Override
	public void initialize() {
		checkForUpdatesCheckbox.setDisable(areUpdatesManagedExternally());
		checkForUpdatesCheckbox.setSelected(settings.checkForUpdates().get() && !areUpdatesManagedExternally());
		portField.setText(String.valueOf(settings.port().intValue()));
		portField.addEventFilter(KeyEvent.KEY_TYPED, this::filterNumericKeyEvents);
		changePortButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(portField.textProperty()));
		changePortButton.disableProperty().bind(Bindings.createBooleanBinding(this::isPortValid, portField.textProperty()).not());
		useIpv6Label.setVisible(SystemUtils.IS_OS_WINDOWS);
		useIpv6Checkbox.setVisible(SystemUtils.IS_OS_WINDOWS);
		useIpv6Checkbox.setSelected(SystemUtils.IS_OS_WINDOWS && settings.useIpv6().get());
		versionLabel.setText(String.format(localization.getString("settings.version.label"), applicationVersion().orElse("SNAPSHOT")));
		prefGvfsSchemeLabel.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.getItems().add("dav");
		prefGvfsScheme.getItems().add("webdav");
		prefGvfsScheme.setValue(settings.preferredGvfsScheme().get());
		debugModeCheckbox.setSelected(settings.debugMode().get());

		settings.checkForUpdates().bind(checkForUpdatesCheckbox.selectedProperty());
		settings.useIpv6().bind(useIpv6Checkbox.selectedProperty());
		settings.preferredGvfsScheme().bind(prefGvfsScheme.valueProperty());
		settings.debugMode().bind(debugModeCheckbox.selectedProperty());
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/settings.fxml");
	}

	private Optional<String> applicationVersion() {
		return Optional.ofNullable(getClass().getPackage().getImplementationVersion());
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
		if (t.getCharacter() == null || t.getCharacter().length() == 0) {
			return;
		}
		char c = CharUtils.toChar(t.getCharacter());
		if (!(CharUtils.isAsciiNumeric(c) || c == '_')) {
			t.consume();
		}
	}

	private boolean areUpdatesManagedExternally() {
		return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	}

}
