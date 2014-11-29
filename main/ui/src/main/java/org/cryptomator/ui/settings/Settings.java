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

import org.apache.commons.lang3.SystemUtils;
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
			SETTINGS_DIR = fs.getPath(appdata, "opencloudencryptor");
		} else if (SystemUtils.IS_OS_WINDOWS && appdata == null) {
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, ".opencloudencryptor");
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, "Library/Application Support/opencloudencryptor");
		} else {
			// (os.contains("solaris") || os.contains("sunos") || os.contains("linux") || os.contains("unix"))
			SETTINGS_DIR = fs.getPath(SystemUtils.USER_HOME, ".opencloudencryptor");
		}
	}

	private String webdavWorkDir;
	private String username;
	private int port;

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
		final Settings result = new Settings();
		result.setWebdavWorkDir(System.getProperty("user.home", "."));
		return result;
	}

	/* Getter/Setter */

	public String getWebdavWorkDir() {
		return webdavWorkDir;
	}

	public void setWebdavWorkDir(String webdavWorkDir) {
		this.webdavWorkDir = webdavWorkDir;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Deprecated
	public int getPort() {
		return port;
	}

	@Deprecated
	public void setPort(int port) {
		this.port = port;
	}

}
