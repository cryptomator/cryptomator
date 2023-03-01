/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.cryptomator.common.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.IOException;

public class SettingsJsonAdapterTest {

	private final Environment env = Mockito.mock(Environment.class);
	private final SettingsJsonAdapter adapter = new SettingsJsonAdapter(env);

	@Test
	public void testDeserialize() throws IOException {
		String json = """
				{
					"directories": [
						{"id": "1", "path": "/vault1", "mountName": "vault1", "winDriveLetter": "X"},
						{"id": "2", "path": "/vault2", "mountName": "vault2", "winDriveLetter": "Y"}
					],
					"autoCloseVaults" : true,
					"checkForUpdatesEnabled": true,
					"port": 8080,
					"language": "de-DE",
					"numTrayNotifications": 42
				}
				""";

		Settings settings = adapter.fromJson(json);

		Assertions.assertTrue(settings.checkForUpdates().get());
		Assertions.assertEquals(2, settings.getDirectories().size());
		Assertions.assertEquals(8080, settings.port().get());
		Assertions.assertEquals(true, settings.autoCloseVaults().get());
		Assertions.assertEquals("de-DE", settings.languageProperty().get());
		Assertions.assertEquals(42, settings.numTrayNotifications().get());
	}

	@SuppressWarnings("SpellCheckingInspection")
	@ParameterizedTest(name = "fromJson() should throw IOException for input: {0}")
	@ValueSource(strings = { //
			"", //
			"<html>", //
			"{invalidjson}" //
	})
	public void testDeserializeMalformed(String input) {
		Assertions.assertThrows(IOException.class, () -> {
			adapter.fromJson(input);
		});
	}

}
