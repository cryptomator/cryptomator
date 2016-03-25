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
import org.fxmisc.easybind.EasyBind;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
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
	private CheckBox useIpv6Checkbox;

	@FXML
	private Label versionLabel;

	@Override
	public void initialize() {
		checkForUpdatesCheckbox.setDisable(areUpdatesManagedExternally());
		checkForUpdatesCheckbox.setSelected(settings.isCheckForUpdatesEnabled() && !areUpdatesManagedExternally());
		portField.setText(String.valueOf(settings.getPort()));
		portField.addEventFilter(KeyEvent.KEY_TYPED, this::filterNumericKeyEvents);
		useIpv6Checkbox.setDisable(!SystemUtils.IS_OS_WINDOWS);
		useIpv6Checkbox.setSelected(SystemUtils.IS_OS_WINDOWS && settings.shouldUseIpv6());
		versionLabel.setText(String.format(localization.getString("settings.version.label"), applicationVersion().orElse("SNAPSHOT")));

		EasyBind.subscribe(checkForUpdatesCheckbox.selectedProperty(), settings::setCheckForUpdatesEnabled);
		EasyBind.subscribe(portField.textProperty(), this::portDidChange);
		EasyBind.subscribe(useIpv6Checkbox.selectedProperty(), settings::setUseIpv6);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/settings.fxml");
	}

	private Optional<String> applicationVersion() {
		return Optional.ofNullable(getClass().getPackage().getImplementationVersion());
	}

	private void portDidChange(String newValue) {
		try {
			int port = Integer.parseInt(newValue);
			if (port < Settings.MIN_PORT) {
				settings.setPort(Settings.DEFAULT_PORT);
			} else if (port < Settings.MAX_PORT) {
				settings.setPort(port);
			} else {
				portField.setText(String.valueOf(Settings.MAX_PORT));
			}
		} catch (NumberFormatException e) {
			portField.setText(String.valueOf(Settings.DEFAULT_PORT));
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
