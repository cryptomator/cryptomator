/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.CharUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Singleton
public class VaultObjectMapperProvider implements Provider<ObjectMapper> {

	private final VaultFactory vaultFactoy;

	@Inject
	public VaultObjectMapperProvider(final VaultFactory vaultFactoy) {
		this.vaultFactoy = vaultFactoy;
	}

	@Override
	public ObjectMapper get() {
		final ObjectMapper om = new ObjectMapper();
		final SimpleModule module = new SimpleModule("VaultJsonMapper");
		module.addSerializer(Vault.class, new VaultSerializer());
		module.addDeserializer(Vault.class, new VaultDeserializer());
		om.registerModule(module);
		return om;
	}

	private static class VaultSerializer extends JsonSerializer<Vault> {

		@Override
		public void serialize(Vault value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeStringField("path", value.path().getValue().toString());
			jgen.writeStringField("mountName", value.getMountName());
			final Character winDriveLetter = value.getWinDriveLetter();
			if (winDriveLetter != null) {
				jgen.writeStringField("winDriveLetter", Character.toString(winDriveLetter));
			}
			jgen.writeEndObject();
		}

	}

	private class VaultDeserializer extends JsonDeserializer<Vault> {

		@Override
		public Vault deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final JsonNode node = jp.readValueAsTree();
			if (node == null || !node.has("path")) {
				throw new InvalidFormatException("Node is null or doesn't contain a path.", node, Vault.class);
			}
			final String pathStr = node.get("path").asText();
			final Path path = FileSystems.getDefault().getPath(pathStr);
			final Vault vault = vaultFactoy.createVault(path);
			if (node.has("mountName")) {
				vault.setMountName(node.get("mountName").asText());
			}
			if (node.has("winDriveLetter")) {
				vault.setWinDriveLetter(CharUtils.toCharacterObject(node.get("winDriveLetter").asText()));
			}
			return vault;
		}

	}

}
