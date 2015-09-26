/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {"directories", "checkForUpdatesEnabled"})
public class Settings implements Serializable {

	private static final long serialVersionUID = 7609959894417878744L;

	private List<Vault> directories;

	private Boolean checkForUpdatesEnabled;

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

}
