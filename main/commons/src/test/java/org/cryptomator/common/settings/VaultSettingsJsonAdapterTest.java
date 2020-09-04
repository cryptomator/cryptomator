/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Paths;

public class VaultSettingsJsonAdapterTest {

	private final VaultSettingsJsonAdapter adapter = new VaultSettingsJsonAdapter();

	@Test
	public void testDeserialize() throws IOException {
		String json = "{\"id\": \"foo\", \"path\": \"/foo/bar\", \"displayName\": \"test\", \"winDriveLetter\": \"X\", \"shouldBeIgnored\": true, \"individualMountPath\": \"/home/test/crypto\", \"mountFlags\":\"--foo --bar\"}";
		JsonReader jsonReader = new JsonReader(new StringReader(json));

		VaultSettings vaultSettings = adapter.read(jsonReader);
		Assertions.assertEquals("foo", vaultSettings.getId());
		Assertions.assertEquals(Paths.get("/foo/bar"), vaultSettings.path().get());
		Assertions.assertEquals("test", vaultSettings.displayName().get());
		Assertions.assertEquals("X", vaultSettings.winDriveLetter().get());
		Assertions.assertEquals("/home/test/crypto", vaultSettings.customMountPath().get());
		Assertions.assertEquals("--foo --bar", vaultSettings.mountFlags().get());


	}

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

		MatcherAssert.assertThat(result, CoreMatchers.containsString("\"id\":\"test\""));
		if(System.getProperty("os.name").contains("Windows")){
			MatcherAssert.assertThat(result, CoreMatchers.containsString("\"path\":\"\\\\foo\\\\bar\""));
		} else {
			MatcherAssert.assertThat(result, CoreMatchers.containsString("\"path\":\"/foo/bar\""));
		}
		MatcherAssert.assertThat(result, CoreMatchers.containsString("\"displayName\":\"mountyMcMountFace\""));
		MatcherAssert.assertThat(result, CoreMatchers.containsString("\"mountFlags\":\"--foo --bar\""));
	}

}
