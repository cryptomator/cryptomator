package org.cryptomator.launcher;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class AdminPropertiesSetterTest {

	private static final Map<String, Object> NO_STRING_CONFIG = Map.of("list", List.of("a", "b", "c"), //
			"map", Map.of("a", 1, "b", 2));

	private static final Map<String, Object> CONFIG = Map.of("kack", "dudel", //
			"list", List.of("a", "b", "c"), //
			"map", Map.of("a", 1, "b", 2));

	@Test
	@DisplayName("Loading valid JSON")
	void loadValidJson(@TempDir Path path) throws IOException {
		try (var adminPropSetterMock = Mockito.mockStatic(AdminPropertiesSetter.class)) {
			adminPropSetterMock.when(() -> AdminPropertiesSetter.log(anyString(), any())).thenAnswer(Answers.RETURNS_DEFAULTS);
			adminPropSetterMock.when(() -> AdminPropertiesSetter.loadAdminProperties(any())).thenCallRealMethod();
			var configPath = path.resolve("config.json");
			setupValidJson(configPath);

			var result = AdminPropertiesSetter.loadAdminProperties(configPath);
			Assertions.assertAll(CONFIG.entrySet().stream().map((e) -> //
					() -> MatcherAssert.assertThat(result, hasEntry(e.getKey(), e.getValue()))));
		}
	}

	@Test
	@DisplayName("Loading not existing file")
	void loadNotExistingFile(@TempDir Path path) {
		try (var adminPropSetterMock = Mockito.mockStatic(AdminPropertiesSetter.class)) {
			adminPropSetterMock.when(() -> AdminPropertiesSetter.log(anyString(), any())).thenAnswer(Answers.RETURNS_DEFAULTS);
			adminPropSetterMock.when(() -> AdminPropertiesSetter.loadAdminProperties(any())).thenCallRealMethod();
			var configPath = path.resolve("config.json");

			var result = AdminPropertiesSetter.loadAdminProperties(configPath);
			MatcherAssert.assertThat(result, anEmptyMap());
			adminPropSetterMock.verify(() -> AdminPropertiesSetter.log(anyString(), any()));
		}
	}

	@Test
	@DisplayName("Loading empty file")
	void loadEmptyFile(@TempDir Path path) throws IOException {
		try (var adminPropSetterMock = Mockito.mockStatic(AdminPropertiesSetter.class)) {
			adminPropSetterMock.when(() -> AdminPropertiesSetter.log(anyString(), any())).thenAnswer(Answers.RETURNS_DEFAULTS);
			adminPropSetterMock.when(() -> AdminPropertiesSetter.loadAdminProperties(any())).thenCallRealMethod();
			var configPath = path.resolve("config.json");
			Files.createFile(configPath);

			var result = AdminPropertiesSetter.loadAdminProperties(configPath);
			MatcherAssert.assertThat(result, anEmptyMap());
			adminPropSetterMock.verify(() -> AdminPropertiesSetter.log(anyString(), any()));
		}
	}

	void setupValidJson(Path p) throws IOException {
		var json = JsonMapper.builder().build();
		try (var out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			json.writerWithDefaultPrettyPrinter().writeValue(out, CONFIG);
		}
	}

	@Test
	@DisplayName("Keys with non-String values are ignored")
	void ignoreValues(@TempDir Path path) {
		try (var adminPropSetterMock = Mockito.mockStatic(AdminPropertiesSetter.class)) {
			adminPropSetterMock.when(() -> AdminPropertiesSetter.log(anyString(), any())).thenAnswer(Answers.RETURNS_DEFAULTS);
			adminPropSetterMock.when(() -> AdminPropertiesSetter.loadAdminProperties(any())).thenReturn(NO_STRING_CONFIG);
			adminPropSetterMock.when(AdminPropertiesSetter::adjustSystemProperties).thenCallRealMethod();

			AdminPropertiesSetter.adjustSystemProperties();
			adminPropSetterMock.verify(() -> AdminPropertiesSetter.log(anyString(), any()), Mockito.times(NO_STRING_CONFIG.size()));
		}
	}


}
