package org.cryptomator.common;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@DisplayName("Environment Variables Test")
class EnvironmentTest {

	private Environment env;

	@BeforeAll
	static void init() {
		System.setProperty("user.home", "/home/testuser");
	}

	@BeforeEach
	void initEach() {
		env = new Environment();
	}

	@Test
	@DisplayName("cryptomator.settingsPath=~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json")
	public void testSettingsPath() {
		System.setProperty("cryptomator.settingsPath", "~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json");

		List<Path> result = env.getSettingsPath().collect(Collectors.toList());
		MatcherAssert.assertThat(result, Matchers.hasSize(2));
		MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/.config/Cryptomator/settings.json"),
				Paths.get("/home/testuser/.Cryptomator/settings.json")));
	}

	@Test
	@DisplayName("cryptomator.ipcPortPath=~/.config/Cryptomator/ipcPort.bin:~/.Cryptomator/ipcPort.bin")
	public void testIpcPortPath() {
		System.setProperty("cryptomator.ipcPortPath", "~/.config/Cryptomator/ipcPort.bin:~/.Cryptomator/ipcPort.bin");

		List<Path> result = env.getIpcPortPath().collect(Collectors.toList());
		MatcherAssert.assertThat(result, Matchers.hasSize(2));
		MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/.config/Cryptomator/ipcPort.bin"),
				Paths.get("/home/testuser/.Cryptomator/ipcPort.bin")));
	}

	@Test
	@DisplayName("cryptomator.keychainPath=~/AppData/Roaming/Cryptomator/keychain.json")
	public void testKeychainPath() {
		System.setProperty("cryptomator.keychainPath", "~/AppData/Roaming/Cryptomator/keychain.json");

		List<Path> result = env.getKeychainPath().collect(Collectors.toList());
		MatcherAssert.assertThat(result, Matchers.hasSize(1));
		MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/AppData/Roaming/Cryptomator/keychain.json")));
	}

	@Test
	@DisplayName("cryptomator.logDir=/foo/bar")
	public void testAbsoluteLogDir() {
		System.setProperty("cryptomator.logDir", "/foo/bar");

		Optional<Path> logDir = env.getLogDir();

		Assertions.assertTrue(logDir.isPresent());
	}

	@Test
	@DisplayName("cryptomator.logDir=~/foo/bar")
	public void testRelativeLogDir() {
		System.setProperty("cryptomator.logDir", "~/foo/bar");

		Optional<Path> logDir = env.getLogDir();

		Assertions.assertTrue(logDir.isPresent());
		Assertions.assertEquals(Paths.get("/home/testuser/foo/bar"), logDir.get());
	}

	@Nested
	@DisplayName("Path Lists")
	class SettingsPath {

		@Test
		@DisplayName("test.path.property=")
		public void testEmptyList() {
			System.setProperty("test.path.property", "");
			List<Path> result = env.getPaths("test.path.property").collect(Collectors.toList());

			MatcherAssert.assertThat(result, Matchers.hasSize(0));
		}

		@Test
		@DisplayName("test.path.property=/foo/bar/test")
		public void testSingleAbsolutePath() {
			System.setProperty("test.path.property", "/foo/bar/test");
			List<Path> result = env.getPaths("test.path.property").collect(Collectors.toList());

			MatcherAssert.assertThat(result, Matchers.hasSize(1));
			MatcherAssert.assertThat(result, Matchers.hasItem(Paths.get("/foo/bar/test")));
		}

		@Test
		@DisplayName("test.path.property=~/test")
		public void testSingleHomeRelativePath() {
			System.setProperty("test.path.property", "~/test");
			List<Path> result = env.getPaths("test.path.property").collect(Collectors.toList());

			MatcherAssert.assertThat(result, Matchers.hasSize(1));
			MatcherAssert.assertThat(result, Matchers.hasItem(Paths.get("/home/testuser/test")));
		}

		@Test
		@DisplayName("test.path.property=~/test:~/test2:/foo/bar/test")
		public void testMultiplePaths() {
			System.setProperty("test.path.property", "~/test:~/test2:/foo/bar/test");
			List<Path> result = env.getPaths("test.path.property").collect(Collectors.toList());

			MatcherAssert.assertThat(result, Matchers.hasSize(3));
			MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/test"),
					Paths.get("/home/testuser/test2"),
					Paths.get("/foo/bar/test")));
		}

	}

}
