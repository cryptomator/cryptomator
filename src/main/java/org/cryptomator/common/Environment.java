package org.cryptomator.common;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Environment {

	private static final Logger LOG = LoggerFactory.getLogger(Environment.class);
	private static final Path RELATIVE_HOME_DIR = Paths.get("~");
	private static final char PATH_LIST_SEP = ':';
	private static final int DEFAULT_MIN_PW_LENGTH = 8;
	private static final String SETTINGS_PATH_PROP_NAME = "cryptomator.settingsPath";
	private static final String IPC_SOCKET_PATH_PROP_NAME = "cryptomator.ipcSocketPath";
	private static final String KEYCHAIN_PATHS_PROP_NAME = "cryptomator.integrationsWin.keychainPaths";
	private static final String P12_PATH_PROP_NAME = "cryptomator.p12Path";
	private static final String LOG_DIR_PROP_NAME = "cryptomator.logDir";
	private static final String MOUNTPOINT_DIR_PROP_NAME = "cryptomator.mountPointsDir";
	private static final String MIN_PW_LENGTH_PROP_NAME = "cryptomator.minPwLength";
	private static final String APP_VERSION_PROP_NAME = "cryptomator.appVersion";
	private static final String BUILD_NUMBER_PROP_NAME = "cryptomator.buildNumber";
	private static final String PLUGIN_DIR_PROP_NAME = "cryptomator.pluginDir";
	private static final String TRAY_ICON_PROP_NAME = "cryptomator.showTrayIcon";

	public Environment() {
		LOG.debug("user.home: {}", System.getProperty("user.home"));
		LOG.debug("java.library.path: {}", System.getProperty("java.library.path"));
		LOG.debug("user.language: {}", System.getProperty("user.language"));
		LOG.debug("user.region: {}", System.getProperty("user.region"));
		LOG.debug("logback.configurationFile: {}", System.getProperty("logback.configurationFile"));
		logCryptomatorSystemProperty(SETTINGS_PATH_PROP_NAME);
		logCryptomatorSystemProperty(IPC_SOCKET_PATH_PROP_NAME);
		logCryptomatorSystemProperty(KEYCHAIN_PATHS_PROP_NAME);
		logCryptomatorSystemProperty(LOG_DIR_PROP_NAME);
		logCryptomatorSystemProperty(PLUGIN_DIR_PROP_NAME);
		logCryptomatorSystemProperty(MOUNTPOINT_DIR_PROP_NAME);
		logCryptomatorSystemProperty(MIN_PW_LENGTH_PROP_NAME);
		logCryptomatorSystemProperty(APP_VERSION_PROP_NAME);
		logCryptomatorSystemProperty(BUILD_NUMBER_PROP_NAME);
		logCryptomatorSystemProperty(TRAY_ICON_PROP_NAME);
		logCryptomatorSystemProperty(P12_PATH_PROP_NAME);
	}

	public static Environment getInstance() {
		final class Holder {
			private static final Environment INSTANCE = new Environment();
		}
		return Holder.INSTANCE;
	}

	private void logCryptomatorSystemProperty(String propertyName) {
		LOG.debug("{}: {}", propertyName, System.getProperty(propertyName));
	}

	public boolean useCustomLogbackConfig() {
		return getPath("logback.configurationFile").map(Files::exists).orElse(false);
	}

	public Stream<Path> getSettingsPath() {
		return getPaths(SETTINGS_PATH_PROP_NAME);
	}

	public Stream<Path> getP12Path() {
		return getPaths(P12_PATH_PROP_NAME);
	}

	public Stream<Path> ipcSocketPath() {
		return getPaths(IPC_SOCKET_PATH_PROP_NAME);
	}

	public Stream<Path> getKeychainPath() {
		return getPaths(KEYCHAIN_PATHS_PROP_NAME);
	}

	public Optional<Path> getLogDir() {
		return getPath(LOG_DIR_PROP_NAME).map(this::replaceHomeDir);
	}

	public Optional<Path> getPluginDir() {
		return getPath(PLUGIN_DIR_PROP_NAME).map(this::replaceHomeDir);
	}

	public Optional<Path> getMountPointsDir() {
		return getPath(MOUNTPOINT_DIR_PROP_NAME).map(this::replaceHomeDir);
	}

	/**
	 * Returns the app version defined in the {@value APP_VERSION_PROP_NAME} property or returns "SNAPSHOT".
	 * @return App version or "SNAPSHOT", if undefined
	 */
	public String getAppVersion() {
		return System.getProperty(APP_VERSION_PROP_NAME, "SNAPSHOT");
	}

	public Optional<String> getBuildNumber() {
		return Optional.ofNullable(System.getProperty(BUILD_NUMBER_PROP_NAME));
	}

	public int getMinPwLength() {
		return getInt(MIN_PW_LENGTH_PROP_NAME, DEFAULT_MIN_PW_LENGTH);
	}

	public boolean showTrayIcon() {
		return Boolean.getBoolean(TRAY_ICON_PROP_NAME);
	}

	private int getInt(String propertyName, int defaultValue) {
		String value = System.getProperty(propertyName);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) { // includes "null" values
			return defaultValue;
		}
	}

	private Optional<Path> getPath(String propertyName) {
		String value = System.getProperty(propertyName);
		return Optional.ofNullable(value).map(Paths::get);
	}

	// visible for testing
	public Path getHomeDir() {
		return getPath("user.home").orElseThrow();
	}

	// visible for testing
	public Stream<Path> getPaths(String propertyName) {
		Stream<String> rawSettingsPaths = getRawList(propertyName, PATH_LIST_SEP);
		return rawSettingsPaths.filter(Predicate.not(Strings::isNullOrEmpty)).map(Paths::get).map(this::replaceHomeDir);
	}

	private Path replaceHomeDir(Path path) {
		if (path.startsWith(RELATIVE_HOME_DIR)) {
			return getHomeDir().resolve(RELATIVE_HOME_DIR.relativize(path));
		} else {
			return path;
		}
	}

	private Stream<String> getRawList(String propertyName, char separator) {
		String value = System.getProperty(propertyName);
		if (value == null) {
			return Stream.empty();
		} else {
			Iterable<String> iter = Splitter.on(separator).split(value);
			Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(iter.iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE);
			return StreamSupport.stream(spliterator, false);
		}
	}
}
