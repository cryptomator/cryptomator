/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class SettingsJsonAdapterTest {

	private final SettingsJsonAdapter adapter = new SettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String vault1Json = "{\"id\": \"1\", \"path\": \"/vault1\", \"mountName\": \"vault1\", \"winDriveLetter\": \"X\"}";
		String vault2Json = "{\"id\": \"2\", \"path\": \"/vault2\", \"mountName\": \"vault2\", \"winDriveLetter\": \"Y\"}";
		String json = "{\"directories\": [" + vault1Json + "," + vault2Json + "]," //
				+ "\"checkForUpdatesEnabled\": true,"//
				+ "\"port\": 8080,"//
				+ "\"useIpv6\": true,"//
				+ "\"numTrayNotifications\": 42}";

		Settings settings = adapter.fromJson(json);

		Assert.assertTrue(settings.checkForUpdates().get());
		Assert.assertEquals(2, settings.getDirectories().size());
		Assert.assertEquals(8080, settings.port().get());
		// Assert.assertTrue(settings.useIpv6().get()); temporarily ignored
		Assert.assertEquals(42, settings.numTrayNotifications().get());
		Assert.assertEquals("dav", settings.preferredGvfsScheme().get());
	}

}
