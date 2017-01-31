/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.settings;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class VaultSettingsJsonAdapterTest {

	private final VaultSettingsJsonAdapter adapter = new VaultSettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		VaultSettings settings = adapter.fromJson("{\"id\": \"foo\", \"path\": \"/foo/bar\", \"mountName\": \"test\", \"winDriveLetter\": \"X\", \"shouldBeIgnored\": true}");
		Assert.assertEquals("foo", settings.getId());
		Assert.assertEquals(Paths.get("/foo/bar"), settings.path().get());
		Assert.assertEquals("test", settings.mountName().get());
		Assert.assertEquals("X", settings.winDriveLetter().get());
	}

}
