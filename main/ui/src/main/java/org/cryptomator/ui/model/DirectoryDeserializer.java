package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class DirectoryDeserializer extends JsonDeserializer<Directory> {

	@Override
	public Directory deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final JsonNode node = jp.readValueAsTree();
		final String pathStr = node.get("path").asText();
		final Path path = FileSystems.getDefault().getPath(pathStr);
		final Directory dir = new Directory(path);
		final boolean verifyFileIntegrity = node.has("checkIntegrity") ? node.get("checkIntegrity").asBoolean() : false;
		dir.setVerifyFileIntegrity(verifyFileIntegrity);
		if (node.has("mountName")) {
			dir.setMountName(node.get("mountName").asText());
		}
		return dir;
	}

}
