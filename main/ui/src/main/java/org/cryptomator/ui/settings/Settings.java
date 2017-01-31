/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.settings;

import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Settings {

	public static final int MIN_PORT = 1024;
	public static final int MAX_PORT = 65535;
	public static final boolean DEFAULT_CHECK_FOR_UDPATES = true;
	public static final int DEFAULT_PORT = 42427;
	public static final boolean DEFAULT_USE_IPV6 = false;
	public static final int DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;
	public static final String DEFAULT_GVFS_SCHEME = "dav";
	public static final boolean DEFAULT_DEBUG_MODE = false;

	private final Consumer<Settings> saveCmd;
	private final ObservableList<VaultSettings> directories = FXCollections.observableArrayList();
	private final BooleanProperty checkForUpdates = new SimpleBooleanProperty(DEFAULT_CHECK_FOR_UDPATES);
	private final IntegerProperty port = new SimpleIntegerProperty(DEFAULT_PORT);
	private final BooleanProperty useIpv6 = new SimpleBooleanProperty(DEFAULT_USE_IPV6);
	private final IntegerProperty numTrayNotifications = new SimpleIntegerProperty(DEFAULT_NUM_TRAY_NOTIFICATIONS);
	private final StringProperty preferredGvfsScheme = new SimpleStringProperty(DEFAULT_GVFS_SCHEME);
	private final BooleanProperty debugMode = new SimpleBooleanProperty(DEFAULT_DEBUG_MODE);

	/**
	 * Package-private constructor; use {@link SettingsProvider}.
	 */
	Settings(Consumer<Settings> saveCmd) {
		this.saveCmd = saveCmd;
		directories.addListener((ListChangeListener.Change<? extends VaultSettings> change) -> this.save());
		checkForUpdates.addListener(this::somethingChanged);
		port.addListener(this::somethingChanged);
		useIpv6.addListener(this::somethingChanged);
		numTrayNotifications.addListener(this::somethingChanged);
		preferredGvfsScheme.addListener(this::somethingChanged);
		debugMode.addListener(this::somethingChanged);
	}

	private void somethingChanged(ObservableValue<?> observable, Object oldValue, Object newValue) {
		this.save();
	}

	public void save() {
		if (saveCmd != null) {
			saveCmd.accept(this);
		}
	}

	/* Getter/Setter */

	public ObservableList<VaultSettings> getDirectories() {
		return directories;
	}

	public BooleanProperty checkForUpdates() {
		return checkForUpdates;
	}

	public IntegerProperty port() {
		return port;
	}

	public BooleanProperty useIpv6() {
		return useIpv6;
	}

	public IntegerProperty numTrayNotifications() {
		return numTrayNotifications;
	}

	public StringProperty preferredGvfsScheme() {
		return preferredGvfsScheme;
	}

	public BooleanProperty debugMode() {
		return debugMode;
	}

}
