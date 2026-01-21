package org.cryptomator.launcher;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;

public class AdminPropertiesSetterTest {

	private static final String PROPS = """
			fruit=banana
			vegetable:kärrot
			method=scan寧""";

	@Test
	@DisplayName("UTF-8 is supported")
	void loadUTF8Properties(@TempDir Path path) throws IOException {
		var config = path.resolve("config.properties");
		setupValidProperties(config);

		var properties = AdminPropertiesSetter.loadAdminProperties(config);
		Assertions.assertAll(List.of( //
				() -> MatcherAssert.assertThat(properties, hasEntry("fruit", "banana")), //
				() -> MatcherAssert.assertThat(properties, hasEntry("vegetable", "kärrot")), //
				() -> MatcherAssert.assertThat(properties, hasEntry("method", "scan寧"))));
	}

	@Test
	@DisplayName("Loading not existing file")
	void loadNotExistingFile(@TempDir Path path) {
		var config = path.resolve("config.properties");
		var properties = AdminPropertiesSetter.loadAdminProperties(config);
		MatcherAssert.assertThat(properties, anEmptyMap());
	}

	@Test
	@DisplayName("Loading invalid properties file")
	void loadEmptyFile(@TempDir Path path) throws IOException {
	}

	void setupValidProperties(Path p) throws IOException {
		try (var out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			var bytes = PROPS.getBytes(StandardCharsets.UTF_8);
			out.write(bytes);
		}
	}

}
