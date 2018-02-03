/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.NioAdapterImpl;

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
	private GridPane webdavNioAdapter;

	@FXML
	private GridPane fuseNioAdapter;

	@FXML
	private Label portFieldLabel;

	@FXML
	private TextField portField;

	@FXML
	private Button changePortButton;

	@FXML
	private Label versionLabel;

	@FXML
	private Label prefGvfsSchemeLabel;

	@FXML
	private Label defaultMountDirLabel;

	@FXML
	private TextField defaultMountDir;

	@FXML
	private Button changeDefaultMountDirButton;

	@FXML
	private ChoiceBox<String> prefGvfsScheme;

	@FXML
	private Label nioAdapterLabel;

	@FXML
	private ChoiceBox<String> nioAdapter;

	@FXML
	private CheckBox debugModeCheckbox;

	@FXML
	private VBox root;

	@Override
	public void initialize() {
		versionLabel.setText(String.format(localization.getString("settings.version.label"), applicationVersion.orElse("SNAPSHOT")));
		checkForUpdatesCheckbox.setDisable(areUpdatesManagedExternally());
		checkForUpdatesCheckbox.setSelected(settings.checkForUpdates().get() && !areUpdatesManagedExternally());

		//NIOADAPTER
		nioAdapter.getItems().addAll(getSupportedAdapters());
		nioAdapter.setValue(settings.usedNioAdapterImpl().get());
		nioAdapter.setVisible(true);
		nioAdapter.getSelectionModel().selectedItemProperty().addListener( (ObservableValue<? extends String> observable, String oldVal, String newVal) -> changeNioView(newVal) );


		//WEBDAV
		webdavNioAdapter.managedProperty().bind(webdavNioAdapter.visibleProperty());
		prefGvfsScheme.managedProperty().bind(webdavNioAdapter.visibleProperty());
		prefGvfsSchemeLabel.managedProperty().bind(webdavNioAdapter.visibleProperty());
		portFieldLabel.managedProperty().bind(webdavNioAdapter.visibleProperty());
		changePortButton.managedProperty().bind(webdavNioAdapter.visibleProperty());
		portField.managedProperty().bind(webdavNioAdapter.visibleProperty());
		portField.setText(String.valueOf(settings.port().intValue()));
		portField.addEventFilter(KeyEvent.KEY_TYPED, this::filterNumericKeyEvents);
		changePortButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(portField.textProperty()));
		changePortButton.disableProperty().bind(Bindings.createBooleanBinding(this::isPortValid, portField.textProperty()).not());
		prefGvfsScheme.getItems().add("dav");
		prefGvfsScheme.getItems().add("webdav");
		prefGvfsScheme.setValue(settings.preferredGvfsScheme().get());
		prefGvfsSchemeLabel.setVisible(SystemUtils.IS_OS_LINUX);
		prefGvfsScheme.setVisible(SystemUtils.IS_OS_LINUX);

		//FUSE
		fuseNioAdapter.managedProperty().bind(fuseNioAdapter.visibleProperty());
		defaultMountDirLabel.managedProperty().bind(fuseNioAdapter.visibleProperty());
		defaultMountDir.managedProperty().bind(fuseNioAdapter.visibleProperty());
		defaultMountDirLabel.setVisible(SystemUtils.IS_OS_LINUX);
		defaultMountDir.setVisible(SystemUtils.IS_OS_LINUX);
		defaultMountDir.setText(String.valueOf(settings.defaultMountDir().get()));
		changeDefaultMountDirButton.setVisible(false);
		changeDefaultMountDirButton.visibleProperty().bind(
				Bindings.createBooleanBinding(
						() ->  fuseNioAdapter.visibleProperty().get() && settings.defaultMountDir().isNotEqualTo(Strings.nullToEmpty(defaultMountDir.getText())).get() ,
						fuseNioAdapter.visibleProperty(),
						settings.defaultMountDir().isNotEqualTo(defaultMountDir.textProperty())
						)
		);
		changeDefaultMountDirButton.disableProperty().bind(Bindings.createBooleanBinding(this::isDirValid, defaultMountDir.textProperty()).not());

		debugModeCheckbox.setSelected(settings.debugMode().get());

		settings.checkForUpdates().bind(checkForUpdatesCheckbox.selectedProperty());
		settings.preferredGvfsScheme().bind(prefGvfsScheme.valueProperty());
		settings.usedNioAdapterImpl().bind(nioAdapter.valueProperty());
		settings.debugMode().bind(debugModeCheckbox.selectedProperty());
	}

	//TODO: how to implement this?
	private String [] getSupportedAdapters() {
		return new String[]{NioAdapterImpl.FUSE.name(), NioAdapterImpl.WEBDAV.name()};
	}

	private void changeNioView(String newVal) {
		fuseNioAdapter.setVisible(newVal.equalsIgnoreCase("FUSE"));
		webdavNioAdapter.setVisible(newVal.equalsIgnoreCase("WEBDAV"));
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

	@FXML
	private void changeDefaultMountDir(ActionEvent event){
		assert isDirValid() : "Error. Not a valid Directory. Does and exist and do you have the needed Rights?";
		settings.defaultMountDir().set(defaultMountDir.getText());
	}

	private boolean isDirValid(){
		if(SystemUtils.IS_OS_WINDOWS){
			//this should never ever happen!
			return false;
		}
		else{
			Path path = Paths.get(defaultMountDir.getText());
			return Files.isDirectory(path) && Files.isReadable(path) && Files.isWritable(path) && Files.isExecutable(path);
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
