/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.model.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder(value = {"webdavWorkDir"})
public class Settings implements Serializable {

	private static final long serialVersionUID = 7609959894417878744L;
	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
	private static final Path SETTINGS_DIR;
	private static final String SETTINGS_FILE = "settings.json";
	private static final ObjectMapper JSON_OM = new ObjectMapper();
	private static Settings INSTANCE = null;

	static {
		final String appdata = System.getenv("APPDATA");
		final FileSystem fs = FileSystems.getDefault();

		if (SystemUtils.IS_OS_WINDOWS && appdata != null) {
			SETTINGS_DIR = fs.getPath(appdata, "Cryptomator");
		} else if (SystemUtils.IS_OS_WINDOWS && appdata == null) {
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, ".Cryptomator");
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, "Library/Application Support/Cryptomator");
		} else {
			// (os.contains("solaris") || os.contains("sunos") || os.contains("linux") || os.contains("unix"))
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, ".Cryptomator");
		}
	}

	private Collection<Directory> directories;
	private String username;

	private Settings() {
		// private constructor
	}

	public static synchronized Settings load() {
		if (INSTANCE == null) {
			try {
				Files.createDirectories(SETTINGS_DIR);
				final Path settingsFile = SETTINGS_DIR.resolve(SETTINGS_FILE);
				final InputStream in = Files.newInputStream(settingsFile, StandardOpenOption.READ);
				INSTANCE = JSON_OM.readValue(in, Settings.class);
				return INSTANCE;
			} catch (IOException e) {
				LOG.warn("Failed to load settings, creating new one.");
				INSTANCE = Settings.defaultSettings();
			}
		}
		return INSTANCE;
	}

	public static synchronized void save() {
		if (INSTANCE != null) {
			try {
				Files.createDirectories(SETTINGS_DIR);
				final Path settingsFile = SETTINGS_DIR.resolve(SETTINGS_FILE);
				final OutputStream out = Files.newOutputStream(settingsFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
				JSON_OM.writeValue(out, INSTANCE);
			} catch (IOException e) {
				LOG.error("Failed to save settings.", e);
			}
		}
	}

	private static Settings defaultSettings() {
		return new Settings();
	}

	/* Getter/Setter */

	public Collection<Directory> getDirectories() {
		if (directories == null) {
			directories = new ArrayList<>();
		}
		return directories;
	}

	public void setDirectories(Collection<Directory> directories) {
		this.directories = directories;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
