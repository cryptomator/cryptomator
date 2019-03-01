/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

public class VaultSettingsJsonAdapterTest {

	private final VaultSettingsJsonAdapter adapter = new VaultSettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String json = "{\"id\": \"foo\", \"path\": \"/foo/bar\", \"mountName\": \"test\", \"winDriveLetter\": \"X\", \"shouldBeIgnored\": true, \"individualMountPath\": \"/home/test/crypto\"}";
		JsonReader jsonReader = new JsonReader(new StringReader(json));

		VaultSettings vaultSettings = adapter.read(jsonReader);
		Assertions.assertEquals("foo", vaultSettings.getId());
		Assertions.assertEquals(Paths.get("/foo/bar"), vaultSettings.path().get());
		Assertions.assertEquals("test", vaultSettings.mountName().get());
		Assertions.assertEquals("X", vaultSettings.winDriveLetter().get());
		Assertions.assertEquals("/home/test/crypto", vaultSettings.individualMountPath().get());
	}

}
