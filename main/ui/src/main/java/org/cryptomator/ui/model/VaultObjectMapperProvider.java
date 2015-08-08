package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

	private class VaultSerializer extends JsonSerializer<Vault> {

		@Override
		public void serialize(Vault value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
			jgen.writeStartObject();
			jgen.writeStringField("path", value.getPath().toString());
			jgen.writeStringField("mountName", value.getMountName().toString());
			jgen.writeEndObject();
		}

	}

	private class VaultDeserializer extends JsonDeserializer<Vault> {

		@Override
		public Vault deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final JsonNode node = jp.readValueAsTree();
			final String pathStr = node.get("path").asText();
			final Path path = FileSystems.getDefault().getPath(pathStr);
			final Vault vault = vaultFactoy.createVault(path);
			if (node.has("mountName")) {
				vault.setMountName(node.get("mountName").asText());
			}
			return vault;
		}

	}

}
