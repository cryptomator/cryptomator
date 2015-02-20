package org.cryptomator.ui.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.DeferredCloser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

public class SettingsProvider implements Provider<Settings> {

	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
	private static final Path SETTINGS_DIR;
	private static final String SETTINGS_FILE = "settings.json";

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

	private final DeferredCloser deferredCloser;
	private final ObjectMapper objectMapper;

	@Inject
	public SettingsProvider(DeferredCloser deferredCloser, @Named("VaultJsonMapper") ObjectMapper objectMapper) {
		this.deferredCloser = deferredCloser;
		this.objectMapper = objectMapper;
	}

	@Override
	public Settings get() {
		Settings settings = null;
		try {
			Files.createDirectories(SETTINGS_DIR);
			final Path settingsFile = SETTINGS_DIR.resolve(SETTINGS_FILE);
			final InputStream in = Files.newInputStream(settingsFile, StandardOpenOption.READ);
			settings = objectMapper.readValue(in, Settings.class);
		} catch (IOException e) {
			LOG.warn("Failed to load settings, creating new one.");
			settings = new Settings();
		}
		deferredCloser.closeLater(settings, this::save);
		return settings;
	}

	private void save(Settings settings) {
		if (settings == null) {
			return;
		}
		try {
			Files.createDirectories(SETTINGS_DIR);
			final Path settingsFile = SETTINGS_DIR.resolve(SETTINGS_FILE);
			final OutputStream out = Files.newOutputStream(settingsFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			objectMapper.writeValue(out, settings);
		} catch (IOException e) {
			LOG.error("Failed to save settings.", e);
		}
	}

}
