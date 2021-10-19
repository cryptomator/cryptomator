/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VaultSettingsJsonAdapterTest {

	private final VaultSettingsJsonAdapter adapter = new VaultSettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String json = "{\"id\": \"foo\", \"path\": \"/foo/bar\", \"displayName\": \"test\", \"winDriveLetter\": \"X\", \"shouldBeIgnored\": true, \"individualMountPath\": \"/home/test/crypto\", \"mountFlags\":\"--foo --bar\"}";
		JsonReader jsonReader = new JsonReader(new StringReader(json));

		VaultSettings vaultSettings = adapter.read(jsonReader);

		assertAll(
				() -> assertEquals("foo", vaultSettings.getId()),
				() -> assertEquals(Paths.get("/foo/bar"), vaultSettings.path().get()),
				() -> assertEquals("test", vaultSettings.displayName().get()),
				() -> assertEquals("X", vaultSettings.winDriveLetter().get()),
				() -> assertEquals("/home/test/crypto", vaultSettings.customMountPath().get()),
				() -> assertEquals("--foo --bar", vaultSettings.mountFlags().get())
		);
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
	public void testSerialize() throws IOException {
		VaultSettings vaultSettings = new VaultSettings("test");
		vaultSettings.path().set(Paths.get("/foo/bar"));
		vaultSettings.displayName().set("mountyMcMountFace");
		vaultSettings.mountFlags().set("--foo --bar");

		StringWriter buf = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(buf);
		adapter.write(jsonWriter, vaultSettings);
		String result = buf.toString();

		assertAll(
				() -> assertThat(result, containsString("\"id\":\"test\"")),
				() -> {
					if (System.getProperty("os.name").contains("Windows")) {
						assertThat(result, containsString("\"path\":\"\\\\foo\\\\bar\""));
					} else {
						assertThat(result, containsString("\"path\":\"/foo/bar\""));
					}
				},
				() -> assertThat(result, containsString("\"displayName\":\"mountyMcMountFace\"")),
				() -> assertThat(result, containsString("\"mountFlags\":\"--foo --bar\""))
		);
	}
}
