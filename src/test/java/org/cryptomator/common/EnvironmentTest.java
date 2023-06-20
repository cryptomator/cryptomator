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

	}

}
