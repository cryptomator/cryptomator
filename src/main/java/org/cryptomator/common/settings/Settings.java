/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.common.settings;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.NodeOrientation;
import java.util.function.Consumer;

public class Settings {

	public static final int MIN_PORT = 1024;
	public static final int MAX_PORT = 65535;
	public static final boolean DEFAULT_ASKED_FOR_UPDATE_CHECK = false;
	public static final boolean DEFAULT_CHECK_FOR_UPDATES = false;
	public static final boolean DEFAULT_START_HIDDEN = false;
	public static final int DEFAULT_PORT = 42427;
	public static final int DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;
	public static final WebDavUrlScheme DEFAULT_GVFS_SCHEME = WebDavUrlScheme.DAV;
	public static final boolean DEFAULT_DEBUG_MODE = false;
	public static final VolumeImpl DEFAULT_PREFERRED_VOLUME_IMPL = SystemUtils.IS_OS_WINDOWS ? VolumeImpl.DOKANY : VolumeImpl.FUSE;
	public static final UiTheme DEFAULT_THEME = UiTheme.LIGHT;
	@Deprecated // to be changed to "whatever is available" eventually
	public static final String DEFAULT_KEYCHAIN_PROVIDER = SystemUtils.IS_OS_WINDOWS ? "org.cryptomator.windows.keychain.WindowsProtectedKeychainAccess" : SystemUtils.IS_OS_MAC ? "org.cryptomator.macos.keychain.MacSystemKeychainAccess" : "org.cryptomator.linux.keychain.SecretServiceKeychainAccess";
	public static final NodeOrientation DEFAULT_USER_INTERFACE_ORIENTATION = NodeOrientation.LEFT_TO_RIGHT;
	public static final String DEFAULT_LICENSE_KEY = "";
	public static final boolean DEFAULT_SHOW_MINIMIZE_BUTTON = false;
	public static final String DEFAULT_DISPLAY_CONFIGURATION = "";


	private final ObservableList<VaultSettings> directories = FXCollections.observableArrayList(VaultSettings::observables);
	private final BooleanProperty askedForUpdateCheck = new SimpleBooleanProperty(DEFAULT_ASKED_FOR_UPDATE_CHECK);
	private final BooleanProperty checkForUpdates = new SimpleBooleanProperty(DEFAULT_CHECK_FOR_UPDATES);
	private final BooleanProperty startHidden = new SimpleBooleanProperty(DEFAULT_START_HIDDEN);
	private final IntegerProperty port = new SimpleIntegerProperty(DEFAULT_PORT);
	private final IntegerProperty numTrayNotifications = new SimpleIntegerProperty(DEFAULT_NUM_TRAY_NOTIFICATIONS);
	private final ObjectProperty<WebDavUrlScheme> preferredGvfsScheme = new SimpleObjectProperty<>(DEFAULT_GVFS_SCHEME);
	private final BooleanProperty debugMode = new SimpleBooleanProperty(DEFAULT_DEBUG_MODE);
	private final ObjectProperty<VolumeImpl> preferredVolumeImpl = new SimpleObjectProperty<>(DEFAULT_PREFERRED_VOLUME_IMPL);
	private final ObjectProperty<UiTheme> theme = new SimpleObjectProperty<>(DEFAULT_THEME);
	private final ObjectProperty<String> keychainProvider = new SimpleObjectProperty<>(DEFAULT_KEYCHAIN_PROVIDER);
	private final ObjectProperty<NodeOrientation> userInterfaceOrientation = new SimpleObjectProperty<>(DEFAULT_USER_INTERFACE_ORIENTATION);
	private final StringProperty licenseKey = new SimpleStringProperty(DEFAULT_LICENSE_KEY);
	private final BooleanProperty showMinimizeButton = new SimpleBooleanProperty(DEFAULT_SHOW_MINIMIZE_BUTTON);
	private final BooleanProperty showTrayIcon;
	private final IntegerProperty windowXPosition = new SimpleIntegerProperty();
	private final IntegerProperty windowYPosition = new SimpleIntegerProperty();
	private final IntegerProperty windowWidth = new SimpleIntegerProperty();
	private final IntegerProperty windowHeight = new SimpleIntegerProperty();
	private final ObjectProperty<String> displayConfiguration = new SimpleObjectProperty<>(DEFAULT_DISPLAY_CONFIGURATION);


	private Consumer<Settings> saveCmd;

	/**
	 * Package-private constructor; use {@link SettingsProvider}.
	 */
	Settings(Environment env) {
		this.showTrayIcon = new SimpleBooleanProperty(env.showTrayIcon());

		directories.addListener(this::somethingChanged);
		askedForUpdateCheck.addListener(this::somethingChanged);
		checkForUpdates.addListener(this::somethingChanged);
		startHidden.addListener(this::somethingChanged);
		port.addListener(this::somethingChanged);
		numTrayNotifications.addListener(this::somethingChanged);
		preferredGvfsScheme.addListener(this::somethingChanged);
		debugMode.addListener(this::somethingChanged);
		preferredVolumeImpl.addListener(this::somethingChanged);
		theme.addListener(this::somethingChanged);
		keychainProvider.addListener(this::somethingChanged);
		userInterfaceOrientation.addListener(this::somethingChanged);
		licenseKey.addListener(this::somethingChanged);
		showMinimizeButton.addListener(this::somethingChanged);
		showTrayIcon.addListener(this::somethingChanged);
		windowXPosition.addListener(this::somethingChanged);
		windowYPosition.addListener(this::somethingChanged);
		windowWidth.addListener(this::somethingChanged);
		windowHeight.addListener(this::somethingChanged);
		displayConfiguration.addListener(this::somethingChanged);
	}

	void setSaveCmd(Consumer<Settings> saveCmd) {
		this.saveCmd = saveCmd;
	}

	private void somethingChanged(@SuppressWarnings("unused") Observable observable) {
		this.save();
	}

	void save() {
		if (saveCmd != null) {
			saveCmd.accept(this);
		}
	}

	/* Getter/Setter */

	public ObservableList<VaultSettings> getDirectories() {
		return directories;
	}

	public BooleanProperty askedForUpdateCheck() {
		return askedForUpdateCheck;
	}

	public BooleanProperty checkForUpdates() {
		return checkForUpdates;
	}

	public BooleanProperty startHidden() {
		return startHidden;
	}

	public IntegerProperty port() {
		return port;
	}

	public IntegerProperty numTrayNotifications() {
		return numTrayNotifications;
	}

	public ObjectProperty<WebDavUrlScheme> preferredGvfsScheme() {
		return preferredGvfsScheme;
	}

	public BooleanProperty debugMode() {
		return debugMode;
	}

	public ObjectProperty<VolumeImpl> preferredVolumeImpl() {
		return preferredVolumeImpl;
	}

	public ObjectProperty<UiTheme> theme() {
		return theme;
	}

	public ObjectProperty<String> keychainProvider() {return keychainProvider;}

	public ObjectProperty<NodeOrientation> userInterfaceOrientation() {
		return userInterfaceOrientation;
	}

	public StringProperty licenseKey() {
		return licenseKey;
	}

	public BooleanProperty showMinimizeButton() {
		return showMinimizeButton;
	}

	public BooleanProperty showTrayIcon() {
		return showTrayIcon;
	}

	public IntegerProperty windowXPositionProperty() {
		return windowXPosition;
	}

	public IntegerProperty windowYPositionProperty() {
		return windowYPosition;
	}

	public IntegerProperty windowWidthProperty() {
		return windowWidth;
	}

	public IntegerProperty windowHeightProperty() {
		return windowHeight;
	}

	public ObjectProperty<String> displayConfigurationProperty() {
		return displayConfiguration;
	}
}
