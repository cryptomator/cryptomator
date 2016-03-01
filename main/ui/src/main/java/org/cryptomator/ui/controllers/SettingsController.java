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
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.settings.Settings;
import org.fxmisc.easybind.EasyBind;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

@Singleton
public class SettingsController extends AbstractFXMLViewController {

	private final Application app;
	private final Settings settings;

	@Inject
	public SettingsController(Application app, Settings settings) {
		this.app = app;
		this.settings = settings;
	}

	@FXML
	private CheckBox checkForUpdatesCheckbox;

	@Override
	public void initialize() {
		checkForUpdatesCheckbox.setSelected(settings.isCheckForUpdatesEnabled());
		EasyBind.subscribe(checkForUpdatesCheckbox.selectedProperty(), settings::setCheckForUpdatesEnabled);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/settings.fxml");
	}

	@Override
	protected ResourceBundle getFxmlResourceBundle() {
		return ResourceBundle.getBundle("localization");
	}

	// private boolean areUpdatesManagedExternally() {
	// return Boolean.parseBoolean(System.getProperty("cryptomator.updatesManagedExternally", "false"));
	// }

}
