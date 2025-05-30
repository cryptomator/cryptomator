package org.cryptomator.common;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@DisplayName("Environment Variables Test")
public class EnvironmentTest {

	private Environment env;

	@BeforeEach
	public void init() {
		env = Mockito.spy(Environment.getInstance());
	}

	@Test
	@DisplayName("cryptomator.logDir=/foo/bar")
	public void testAbsoluteLogDir() {
		System.setProperty("cryptomator.logDir", "/foo/bar");

		Optional<Path> logDir = env.getLogDir();

		Assertions.assertTrue(logDir.isPresent());
	}

	@Nested
	@DisplayName("Testing parsing path lists")
	public class PathLists {

		@Test
		@DisplayName("test.path.property=")
		public void testEmptyList() {
			System.setProperty("test.path.property", "");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(0));
		}

		@Test
		@DisplayName("test.path.property=/foo/bar/test")
		public void testSinglePath() {
			System.setProperty("test.path.property", "/foo/bar/test");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(1));
			MatcherAssert.assertThat(result, Matchers.hasItem(Paths.get("/foo/bar/test")));
		}

		@Test
		@EnabledIf("isColonPathSeperator")
		@DisplayName("test.path.property=/foo/bar/test:/bar/nez/tost")
		public void testTwoPathsColon() {
			System.setProperty("test.path.property", "/foo/bar/test:bar/nez/tost");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(2));
			MatcherAssert.assertThat(result, Matchers.hasItems(Path.of("/foo/bar/test"), Path.of("bar/nez/tost")));
		}

		@Test
		@EnabledIf("isSemiColonPathSeperator")
		@DisplayName("test.path.property=/foo/bar/test;/bar/nez/tost")
		public void testTwoPathsSemiColon() {
			System.setProperty("test.path.property", "/foo/bar/test;bar/nez/tost");
			List<Path> result = env.getPaths("test.path.property").toList();

			MatcherAssert.assertThat(result, Matchers.hasSize(2));
			MatcherAssert.assertThat(result, Matchers.hasItems(Path.of("/foo/bar/test"), Path.of("bar/nez/tost")));
		}

		boolean isColonPathSeperator() {
			return System.getProperty("path.separator").equals(":");
		}

		boolean isSemiColonPathSeperator() {
			return System.getProperty("path.separator").equals(";");
		}

	}

	@Nested
	public class VariablesContainingPathLists {

		@Test
		public void testSettingsPath() {
			Mockito.doReturn(Stream.of()).when(env).getPaths(Mockito.anyString());
			env.getSettingsPath();
			Mockito.verify(env).getPaths("cryptomator.settingsPath");
		}

		@Test
		public void testP12Path() {
			Mockito.doReturn(Stream.of()).when(env).getPaths(Mockito.anyString());
			env.getP12Path();
			Mockito.verify(env).getPaths("cryptomator.p12Path");
		}

		@Test
		public void testIpcSocketPath() {
			Mockito.doReturn(Stream.of()).when(env).getPaths(Mockito.anyString());
			env.getIpcSocketPath();
			Mockito.verify(env).getPaths("cryptomator.ipcSocketPath");
		}

		@Test
		public void testKeychainPath() {
			Mockito.doReturn(Stream.of()).when(env).getPaths(Mockito.anyString());
			env.getKeychainPath();
			Mockito.verify(env).getPaths("cryptomator.integrationsWin.keychainPaths");
		}
	}

}
