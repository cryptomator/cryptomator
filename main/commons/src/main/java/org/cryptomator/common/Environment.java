package org.cryptomator.common;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Singleton
public class Environment {

	private static final Logger LOG = LoggerFactory.getLogger(Environment.class);
	private static final String USER_HOME = System.getProperty("user.home");
	private static final Path RELATIVE_HOME_DIR = Paths.get("~");
	private static final Path ABSOLUTE_HOME_DIR = Paths.get(USER_HOME);
	private static final char PATH_LIST_SEP = ':';

	@Inject
	public Environment() {
		LOG.debug("user.language: {}", System.getProperty("user.language"));
		LOG.debug("user.region: {}", System.getProperty("user.region"));
		LOG.debug("logback.configurationFile: {}", System.getProperty("logback.configurationFile"));
		LOG.debug("cryptomator.settingsPath: {}", System.getProperty("cryptomator.settingsPath"));
		LOG.debug("cryptomator.ipcPortPath: {}", System.getProperty("cryptomator.ipcPortPath"));
		LOG.debug("cryptomator.keychainPath: {}", System.getProperty("cryptomator.keychainPath"));
		LOG.debug("cryptomator.logDir: {}", System.getProperty("cryptomator.logDir"));
		LOG.debug("cryptomator.mountPointsDir: {}", System.getProperty("cryptomator.mountPointsDir"));
	}

	public boolean useCustomLogbackConfig() {
		return getPath("logback.configurationFile").map(Files::exists).orElse(false);
	}

	public Stream<Path> getSettingsPath() {
		return getPaths("cryptomator.settingsPath");
	}

	public Stream<Path> getIpcPortPath() {
		return getPaths("cryptomator.ipcPortPath");
	}

	public Stream<Path> getKeychainPath() {
		return getPaths("cryptomator.keychainPath");
	}

	public Optional<Path> getLogDir() {
		return getPath("cryptomator.logDir").map(this::replaceHomeDir);
	}

	public Optional<Path> getMountPointsDir() {
		return getPath("cryptomator.mountPointsDir").map(this::replaceHomeDir);
	}

	private Optional<Path> getPath(String propertyName) {
		String value = System.getProperty(propertyName);
		return Optional.ofNullable(value).map(Paths::get);
	}

	// visible for testing
	Stream<Path> getPaths(String propertyName) {
		Stream<String> rawSettingsPaths = getRawList(propertyName, PATH_LIST_SEP);
		return rawSettingsPaths.filter(Predicate.not(Strings::isNullOrEmpty)).map(Paths::get).map(this::replaceHomeDir);
	}

	private Path replaceHomeDir(Path path) {
		if (path.startsWith(RELATIVE_HOME_DIR)) {
			return ABSOLUTE_HOME_DIR.resolve(RELATIVE_HOME_DIR.relativize(path));
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
			Spliterator<String> spliter = Spliterators.spliteratorUnknownSize(iter.iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE);
			return StreamSupport.stream(spliter, false);
		}
	}

}
