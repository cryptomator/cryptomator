/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Settings {

	public static final int MIN_PORT = 1024;
	public static final int MAX_PORT = 65535;
	public static final int DEFAULT_PORT = 42427;
	public static final boolean DEFAULT_USE_IPV6 = false;
	public static final Integer DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;
	public static final String DEFAULT_GVFS_SCHEME = "dav";
	public static final boolean DEFAULT_DEBUG_MODE = false;

	private final Consumer<Settings> saveCmd;

	@Expose
	@SerializedName("directories")
	private List<VaultSettings> directories;

	@Expose
	@SerializedName("checkForUpdatesEnabled")
	private Boolean checkForUpdatesEnabled;

	@Expose
	@SerializedName("port")
	private Integer port;

	@Expose
	@SerializedName("useIpv6")
	private Boolean useIpv6;

	@Expose
	@SerializedName("numTrayNotifications")
	private Integer numTrayNotifications;

	@Expose
	@SerializedName("preferredGvfsScheme")
	private String preferredGvfsScheme;

	@Expose
	@SerializedName("debugMode")
	private Boolean debugMode;

	/**
	 * Package-private constructor; use {@link SettingsProvider}.
	 */
	Settings(Consumer<Settings> saveCmd) {
		this.saveCmd = saveCmd;
	}

	public void save() {
		if (saveCmd != null) {
			saveCmd.accept(this);
		}
	}

	/* Getter/Setter */

	public List<VaultSettings> getDirectories() {
		if (directories == null) {
			directories = new ArrayList<>();
		}
		return directories;
	}

	public void setDirectories(List<VaultSettings> directories) {
		this.directories = directories;
	}

	public boolean isCheckForUpdatesEnabled() {
		// not false meaning "null or true", so that true is the default value, if not setting exists yet.
		return !Boolean.FALSE.equals(checkForUpdatesEnabled);
	}

	public void setCheckForUpdatesEnabled(boolean checkForUpdatesEnabled) {
		this.checkForUpdatesEnabled = checkForUpdatesEnabled;
	}

	public void setPort(int port) {
		if (!isPortValid(port)) {
			throw new IllegalArgumentException("Invalid port");
		}
		this.port = port;
	}

	public int getPort() {
		if (port == null || !isPortValid(port)) {
			return DEFAULT_PORT;
		} else {
			return port;
		}
	}

	public boolean isPortValid(int port) {
		return port == DEFAULT_PORT || port >= MIN_PORT && port <= MAX_PORT || port == 0;
	}

	public boolean shouldUseIpv6() {
		return useIpv6 == null ? DEFAULT_USE_IPV6 : useIpv6;
	}

	public void setUseIpv6(boolean useIpv6) {
		this.useIpv6 = useIpv6;
	}

	public Integer getNumTrayNotifications() {
		return numTrayNotifications == null ? DEFAULT_NUM_TRAY_NOTIFICATIONS : numTrayNotifications;
	}

	public void setNumTrayNotifications(Integer numTrayNotifications) {
		this.numTrayNotifications = numTrayNotifications;
	}

	public String getPreferredGvfsScheme() {
		return preferredGvfsScheme == null ? DEFAULT_GVFS_SCHEME : preferredGvfsScheme;
	}

	public void setPreferredGvfsScheme(String preferredGvfsScheme) {
		this.preferredGvfsScheme = preferredGvfsScheme;
	}

	public boolean getDebugMode() {
		return debugMode == null ? DEFAULT_DEBUG_MODE : debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

}
