/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.stream.JsonReader;

public class VaultSettingsJsonAdapterTest {

	private final VaultSettingsJsonAdapter adapter = new VaultSettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String json = "{\"id\": \"foo\", \"path\": \"/foo/bar\", \"mountName\": \"test\", \"winDriveLetter\": \"X\", \"shouldBeIgnored\": true, \"individualMountPath\": \"/home/test/crypto\"}";
		JsonReader jsonReader = new JsonReader(new StringReader(json));

		VaultSettings vaultSettings = adapter.read(jsonReader);
		Assert.assertEquals("foo", vaultSettings.getId());
		Assert.assertEquals(Paths.get("/foo/bar"), vaultSettings.path().get());
		Assert.assertEquals("test", vaultSettings.mountName().get());
		Assert.assertEquals("X", vaultSettings.winDriveLetter().get());
		Assert.assertEquals("/home/test/crypto", vaultSettings.individualMountPath().get());
	}

}
