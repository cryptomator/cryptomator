/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.cryptomator.ui.model.Vault;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"directories", "checkForUpdatesEnabled", "port", "useIpv6", "numTrayNotifications"})
public class Settings implements Serializable {

	private static final long serialVersionUID = 7609959894417878744L;
	public static final int MIN_PORT = 1024;
	public static final int MAX_PORT = 65535;
	public static final int DEFAULT_PORT = 0;
	public static final boolean DEFAULT_USE_IPV6 = false;
	public static final Integer DEFAULT_NUM_TRAY_NOTIFICATIONS = 3;

	@JsonProperty("directories")
	private List<Vault> directories;

	@JsonProperty("checkForUpdatesEnabled")
	private Boolean checkForUpdatesEnabled;

	@JsonProperty("port")
	private Integer port;
	
	@JsonProperty("useIpv6")
	private Boolean useIpv6;

	@JsonProperty("numTrayNotifications")
	private Integer numTrayNotifications;

	/**
	 * Package-private constructor; use {@link SettingsProvider}.
	 */
	Settings() {

	}

	/* Getter/Setter */

	public List<Vault> getDirectories() {
		if (directories == null) {
			directories = new ArrayList<>();
		}
		return directories;
	}

	public void setDirectories(List<Vault> directories) {
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

	private boolean isPortValid(int port) {
		return port == DEFAULT_PORT || port >= MIN_PORT && port <= MAX_PORT;
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

}
