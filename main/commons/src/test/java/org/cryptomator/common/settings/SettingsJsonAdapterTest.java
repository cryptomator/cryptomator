/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SettingsJsonAdapterTest {

	private final SettingsJsonAdapter adapter = new SettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String vault1Json = "{\"id\": \"1\", \"path\": \"/vault1\", \"mountName\": \"vault1\", \"winDriveLetter\": \"X\"}";
		String vault2Json = "{\"id\": \"2\", \"path\": \"/vault2\", \"mountName\": \"vault2\", \"winDriveLetter\": \"Y\"}";
		String json = "{\"directories\": [" + vault1Json + "," + vault2Json + "]," //
				+ "\"checkForUpdatesEnabled\": true,"//
				+ "\"port\": 8080,"//
				+ "\"numTrayNotifications\": 42,"//
				+ "\"preferredVolumeImpl\": \"FUSE\"}";

		Settings settings = adapter.fromJson(json);

		Assertions.assertTrue(settings.checkForUpdates().get());
		Assertions.assertEquals(2, settings.getDirectories().size());
		Assertions.assertEquals(8080, settings.port().get());
		Assertions.assertEquals(42, settings.numTrayNotifications().get());
		Assertions.assertEquals("dav", settings.preferredGvfsScheme().get());
		Assertions.assertEquals(VolumeImpl.FUSE, settings.preferredVolumeImpl().get());
	}

}
