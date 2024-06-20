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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.Instant;
import java.util.function.Consumer;

public class Settings {

	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

	static final boolean DEFAULT_ASKED_FOR_UPDATE_CHECK = false;
	static final boolean DEFAULT_CHECK_FOR_UPDATES = false;
	static final boolean DEFAULT_START_HIDDEN = false;
	static final boolean DEFAULT_AUTO_CLOSE_VAULTS = false;
	static final boolean DEFAULT_USE_KEYCHAIN = true;
	static final int DEFAULT_PORT = 42427;
	static final int DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;
	static final boolean DEFAULT_DEBUG_MODE = false;
	static final UiTheme DEFAULT_THEME = UiTheme.LIGHT;
	static final boolean DEFAULT_ADD_TO_QUICK_ACCESS = true;
	@Deprecated // to be changed to "whatever is available" eventually
	static final String DEFAULT_KEYCHAIN_PROVIDER = SystemUtils.IS_OS_WINDOWS ? "org.cryptomator.windows.keychain.WindowsProtectedKeychainAccess" : SystemUtils.IS_OS_MAC ? "org.cryptomator.macos.keychain.MacSystemKeychainAccess" : "org.cryptomator.linux.keychain.SecretServiceKeychainAccess";
	static final String DEFAULT_USER_INTERFACE_ORIENTATION = NodeOrientation.LEFT_TO_RIGHT.name();
	static final boolean DEFAULT_SHOW_MINIMIZE_BUTTON = false;
	public static final Instant DEFAULT_TIMESTAMP = Instant.parse("2000-01-01T00:00:00Z");
	public final ObservableList<VaultSettings> directories;
	public final BooleanProperty askedForUpdateCheck;
	public final BooleanProperty checkForUpdates;
	public final BooleanProperty startHidden;
	public final BooleanProperty autoCloseVaults;
	public final BooleanProperty useKeychain;
	public final BooleanProperty addToQuickAccess; //TODO: for now, we only support Sidebar integration per System (GNOME Nautilus for Linux)
	public final IntegerProperty port;
	public final IntegerProperty numTrayNotifications;
	public final BooleanProperty debugMode;
	public final ObjectProperty<UiTheme> theme;
	public final StringProperty keychainProvider;
	public final ObjectProperty<NodeOrientation> userInterfaceOrientation;
	public final StringProperty licenseKey;
	public final BooleanProperty showMinimizeButton;
	public final BooleanProperty showTrayIcon;
	public final IntegerProperty windowXPosition;
	public final IntegerProperty windowYPosition;
	public final IntegerProperty windowWidth;
	public final IntegerProperty windowHeight;
	public final StringProperty language;
	public final StringProperty mountService;
	public final ObjectProperty<Instant> lastSuccessfulUpdateCheck;

	private Consumer<Settings> saveCmd;

	public static Settings create(Environment env) {
		var defaults = new SettingsJson();
		defaults.showTrayIcon = env.showTrayIcon();
		return new Settings(defaults);
	}

	/**
	 * Recreate settings from json
	 *
	 * @param json The parsed settings.json
	 */
	Settings(SettingsJson json) {
		this.directories = FXCollections.observableArrayList(VaultSettings::observables);
		this.askedForUpdateCheck = new SimpleBooleanProperty(this, "askedForUpdateCheck", json.askedForUpdateCheck);
		this.checkForUpdates = new SimpleBooleanProperty(this, "checkForUpdates", json.checkForUpdatesEnabled);
		this.startHidden = new SimpleBooleanProperty(this, "startHidden", json.startHidden);
		this.autoCloseVaults = new SimpleBooleanProperty(this, "autoCloseVaults", json.autoCloseVaults);
		this.useKeychain = new SimpleBooleanProperty(this, "useKeychain", json.useKeychain);
		this.addToQuickAccess = new SimpleBooleanProperty(this, "addToQuickAccess", json.addToQuickAccess);
		this.port = new SimpleIntegerProperty(this, "webDavPort", json.port);
		this.numTrayNotifications = new SimpleIntegerProperty(this, "numTrayNotifications", json.numTrayNotifications);
		this.debugMode = new SimpleBooleanProperty(this, "debugMode", json.debugMode);
		this.theme = new SimpleObjectProperty<>(this, "theme", json.theme);
		this.keychainProvider = new SimpleStringProperty(this, "keychainProvider", json.keychainProvider);
		this.userInterfaceOrientation = new SimpleObjectProperty<>(this, "userInterfaceOrientation", parseEnum(json.uiOrientation, NodeOrientation.class, NodeOrientation.LEFT_TO_RIGHT));
		this.licenseKey = new SimpleStringProperty(this, "licenseKey", json.licenseKey);
		this.showMinimizeButton = new SimpleBooleanProperty(this, "showMinimizeButton", json.showMinimizeButton);
		this.showTrayIcon = new SimpleBooleanProperty(this, "showTrayIcon", json.showTrayIcon);
		this.windowXPosition = new SimpleIntegerProperty(this, "windowXPosition", json.windowXPosition);
		this.windowYPosition = new SimpleIntegerProperty(this, "windowYPosition", json.windowYPosition);
		this.windowWidth = new SimpleIntegerProperty(this, "windowWidth", json.windowWidth);
		this.windowHeight = new SimpleIntegerProperty(this, "windowHeight", json.windowHeight);
		this.language = new SimpleStringProperty(this, "language", json.language);
		this.mountService = new SimpleStringProperty(this, "mountService", json.mountService);
		this.lastSuccessfulUpdateCheck = new SimpleObjectProperty<>(this, "lastSuccessfulUpdateCheck", json.lastSuccessfulUpdateCheck);

		this.directories.addAll(json.directories.stream().map(VaultSettings::new).toList());

		migrateLegacySettings(json);

		directories.addListener(this::somethingChanged);
		askedForUpdateCheck.addListener(this::somethingChanged);
		checkForUpdates.addListener(this::somethingChanged);
		startHidden.addListener(this::somethingChanged);
		autoCloseVaults.addListener(this::somethingChanged);
		useKeychain.addListener(this::somethingChanged);
		addToQuickAccess.addListener(this::somethingChanged);
		port.addListener(this::somethingChanged);
		numTrayNotifications.addListener(this::somethingChanged);
		debugMode.addListener(this::somethingChanged);
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
		language.addListener(this::somethingChanged);
		mountService.addListener(this::somethingChanged);
		lastSuccessfulUpdateCheck.addListener(this::somethingChanged);
	}

	@SuppressWarnings("deprecation")
	private void migrateLegacySettings(SettingsJson json) {
		// implicit migration of 1.6.x legacy setting "preferredVolumeImpl":
		if (this.mountService.get() == null && json.preferredVolumeImpl != null) {
			this.mountService.set(switch (json.preferredVolumeImpl) {
				case "Dokany" -> "org.cryptomator.frontend.dokany.mount.DokanyMountProvider";
				case "FUSE" -> {
					if (SystemUtils.IS_OS_WINDOWS) {
						yield "org.cryptomator.frontend.fuse.mount.WinFspNetworkMountProvider";
					} else if (SystemUtils.IS_OS_MAC) {
						yield "org.cryptomator.frontend.fuse.mount.MacFuseMountProvider";
					} else {
						yield "org.cryptomator.frontend.fuse.mount.LinuxFuseMountProvider";
					}
				}
				default -> {
					if (SystemUtils.IS_OS_WINDOWS) {
						yield "org.cryptomator.frontend.webdav.mount.WindowsMounter";
					} else if (SystemUtils.IS_OS_MAC) {
						yield "org.cryptomator.frontend.webdav.mount.MacAppleScriptMounter";
					} else {
						yield "org.cryptomator.frontend.webdav.mount.LinuxGioMounter";
					}
				}
			});
		}
	}

	SettingsJson serialized() {
		var json = new SettingsJson();
		json.directories = directories.stream().map(VaultSettings::serialized).toList();
		json.askedForUpdateCheck = askedForUpdateCheck.get();
		json.checkForUpdatesEnabled = checkForUpdates.get();
		json.startHidden = startHidden.get();
		json.autoCloseVaults = autoCloseVaults.get();
		json.useKeychain = useKeychain.get();
		json.addToQuickAccess = addToQuickAccess.get();
		json.port = port.get();
		json.numTrayNotifications = numTrayNotifications.get();
		json.debugMode = debugMode.get();
		json.theme = theme.get();
		json.keychainProvider = keychainProvider.get();
		json.uiOrientation = userInterfaceOrientation.get().name();
		json.licenseKey = licenseKey.get();
		json.showMinimizeButton = showMinimizeButton.get();
		json.showTrayIcon = showTrayIcon.get();
		json.windowXPosition = windowXPosition.get();
		json.windowYPosition = windowYPosition.get();
		json.windowWidth = windowWidth.get();
		json.windowHeight = windowHeight.get();
		json.language = language.get();
		json.mountService = mountService.get();
		json.lastSuccessfulUpdateCheck = lastSuccessfulUpdateCheck.get();
		return json;
	}

	private <E extends Enum<E>> E parseEnum(String value, Class<E> clazz, E defaultValue) {
		try {
			return Enum.valueOf(clazz, value.toUpperCase());
		} catch (IllegalArgumentException e) {
			LOG.warn("No value {}.{}. Defaulting to {}.", clazz.getSimpleName(), value, defaultValue);
			return defaultValue;
		}
	}


	// TODO rename to setChangeListener
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

}
