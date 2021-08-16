package org.cryptomator.common;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@DisplayName("Environment Variables Test")
public class EnvironmentTest {

	private Environment env;

	@BeforeEach
	public void init() {
		env = Mockito.spy(new Environment());
		Mockito.when(env.getHomeDir()).thenReturn(Path.of("/home/testuser"));
	}

	@Test
	@DisplayName("cryptomator.settingsPath=~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json")
	public void testSettingsPath() {
		System.setProperty("cryptomator.settingsPath", "~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json");

		List<Path> result = env.getSettingsPath().toList();
		MatcherAssert.assertThat(result, Matchers.hasSize(2));
		MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/.config/Cryptomator/settings.json"), //
				Paths.get("/home/testuser/.Cryptomator/settings.json")));
	}

	@Test
	@DisplayName("cryptomator.ipcSocketPath=~/.config/Cryptomator/ipc.socket:~/.Cryptomator/ipc.socket")
	public void testIpcSocketPath() {
		System.setProperty("cryptomator.ipcSocketPath", "~/.config/Cryptomator/ipc.socket:~/.Cryptomator/ipc.socket");

		List<Path> result = env.ipcSocketPath().toList();
		MatcherAssert.assertThat(result, Matchers.hasSize(2));
		MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/.config/Cryptomator/ipc.socket"), //
				Paths.get("/home/testuser/.Cryptomator/ipc.socket")));
	}

	@Test
	@DisplayName("cryptomator.keychainPath=~/AppData/Roaming/Cryptomator/keychain.json")
	public void testKeychainPath() {
		System.setProperty("cryptomator.keychainPath", "~/AppData/Roaming/Cryptomator/keychain.json");

		List<Path> result = env.getKeychainPath().toList();
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
	public class SettingsPath {

		@Test
		@DisplayName("test.path.property=")
		public void testEmptyList() {
			System.setProperty("test.path.property", "");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(0));
		}

		@Test
		@DisplayName("test.path.property=/foo/bar/test")
		public void testSingleAbsolutePath() {
			System.setProperty("test.path.property", "/foo/bar/test");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(1));
			MatcherAssert.assertThat(result, Matchers.hasItem(Paths.get("/foo/bar/test")));
		}

		@Test
		@DisplayName("test.path.property=~/test")
		public void testSingleHomeRelativePath() {
			System.setProperty("test.path.property", "~/test");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(1));
			MatcherAssert.assertThat(result, Matchers.hasItem(Paths.get("/home/testuser/test")));
		}

		@Test
		@DisplayName("test.path.property=~/test:~/test2:/foo/bar/test")
		public void testMultiplePaths() {
			System.setProperty("test.path.property", "~/test:~/test2:/foo/bar/test");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(3));
			MatcherAssert.assertThat(result, Matchers.contains(Paths.get("/home/testuser/test"), //
					Paths.get("/home/testuser/test2"), //
					Paths.get("/foo/bar/test")));
		}

	}

}
