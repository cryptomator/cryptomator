/*******************************************************************************
 * Copyright (c) 2014, 2017 Sebastian Stenzel
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.common.settings;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.function.Consumer;

public class Settings {

	public static final int MIN_PORT = 1024;
	public static final int MAX_PORT = 65535;
	public static final boolean DEFAULT_ASKED_FOR_UPDATE_CHECK = false;
	public static final boolean DEFAULT_CHECK_FOR_UDPATES = false;
	public static final int DEFAULT_PORT = 42427;
	public static final int DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;
	public static final String DEFAULT_GVFS_SCHEME = "dav";
	public static final boolean DEFAULT_DEBUG_MODE = false;
	public static final VolumeImpl DEFAULT_PREFERRED_VOLUME_IMPL = System.getProperty("os.name").toLowerCase().contains("windows") ? VolumeImpl.DOKANY : VolumeImpl.FUSE;

	private final ObservableList<VaultSettings> directories = FXCollections.observableArrayList(VaultSettings::observables);
	private final BooleanProperty askedForUpdateCheck = new SimpleBooleanProperty(DEFAULT_ASKED_FOR_UPDATE_CHECK);
	private final BooleanProperty checkForUpdates = new SimpleBooleanProperty(DEFAULT_CHECK_FOR_UDPATES);
	private final IntegerProperty port = new SimpleIntegerProperty(DEFAULT_PORT);
	private final IntegerProperty numTrayNotifications = new SimpleIntegerProperty(DEFAULT_NUM_TRAY_NOTIFICATIONS);
	private final StringProperty preferredGvfsScheme = new SimpleStringProperty(DEFAULT_GVFS_SCHEME);
	private final BooleanProperty debugMode = new SimpleBooleanProperty(DEFAULT_DEBUG_MODE);
	private final ObjectProperty<VolumeImpl> preferredVolumeImpl = new SimpleObjectProperty<>(DEFAULT_PREFERRED_VOLUME_IMPL);

	private Consumer<Settings> saveCmd;

	/**
	 * Package-private constructor; use {@link SettingsProvider}.
	 */
	Settings() {
		directories.addListener((ListChangeListener.Change<? extends VaultSettings> change) -> this.save());
		askedForUpdateCheck.addListener(this::somethingChanged);
		checkForUpdates.addListener(this::somethingChanged);
		port.addListener(this::somethingChanged);
		numTrayNotifications.addListener(this::somethingChanged);
		preferredGvfsScheme.addListener(this::somethingChanged);
		debugMode.addListener(this::somethingChanged);
		preferredVolumeImpl.addListener(this::somethingChanged);
	}

	void setSaveCmd(Consumer<Settings> saveCmd) {
		this.saveCmd = saveCmd;
	}

	private void somethingChanged(ObservableValue<?> observable, Object oldValue, Object newValue) {
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

	public IntegerProperty port() {
		return port;
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

	public ObjectProperty<VolumeImpl> preferredVolumeImpl() {
		return preferredVolumeImpl;
	}

}
