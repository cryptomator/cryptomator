package org.cryptomator.common.settings;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;

public class SettingsJsonTest {

	@Test
	public void testDeserialize() throws IOException {
		String jsonStr = """
				{
					"directories": [
						{"id": "1", "path": "/vault1", "mountName": "vault1", "winDriveLetter": "X", "shouldBeIgnored": true},
						{"id": "2", "path": "/vault2", "mountName": "vault2", "winDriveLetter": "Y", "mountFlags":"--foo --bar"}
					],
					"autoCloseVaults" : true,
					"checkForUpdatesEnabled": true,
					"port": 8080,
					"language": "de-DE",
					"numTrayNotifications": 42
				}
				""";

		var jsonObj = new ObjectMapper().reader().readValue(jsonStr, SettingsJson.class);

		Assertions.assertTrue(jsonObj.checkForUpdatesEnabled);
		Assertions.assertEquals(2, jsonObj.directories.size());
		Assertions.assertEquals("/vault1", jsonObj.directories.get(0).path);
		Assertions.assertEquals("/vault2", jsonObj.directories.get(1).path);
		Assertions.assertEquals("--foo --bar", jsonObj.directories.get(1).mountFlags);
		Assertions.assertEquals(8080, jsonObj.port);
		Assertions.assertTrue(jsonObj.autoCloseVaults);
		Assertions.assertEquals("de-DE", jsonObj.language);
		Assertions.assertEquals(42, jsonObj.numTrayNotifications);
	}

	@SuppressWarnings("SpellCheckingInspection")
	@ParameterizedTest(name = "throw JacksonException for input: {0}")
	@ValueSource(strings = { //
			"", //
			"<html>", //
			"{invalidjson}" //
	})
	public void testDeserializeMalformed(String input) {
		var objectMapper = new ObjectMapper().reader();

		Assertions.assertThrows(JacksonException.class, () -> {
			objectMapper.readValue(input, SettingsJson.class);
		});
	}

	@Test
	public void testSerialize() throws JsonProcessingException {
		var jsonObj = new SettingsJson();
		jsonObj.directories = List.of(new VaultSettingsJson(), new VaultSettingsJson());
		jsonObj.directories.get(0).id = "test";
		jsonObj.theme = UiTheme.DARK;
		jsonObj.showTrayIcon = false;

		var jsonStr = new ObjectMapper().registerModule(new JavaTimeModule()).writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);

		MatcherAssert.assertThat(jsonStr, containsString("\"theme\" : \"DARK\""));
		MatcherAssert.assertThat(jsonStr, containsString("\"showTrayIcon\" : false"));
		MatcherAssert.assertThat(jsonStr, containsString("\"useKeychain\" : true"));
		MatcherAssert.assertThat(jsonStr, containsString("\"actionAfterUnlock\" : \"ASK\""));
	}

}