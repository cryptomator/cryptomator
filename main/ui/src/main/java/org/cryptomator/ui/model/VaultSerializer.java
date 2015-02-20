package org.cryptomator.ui.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class VaultSerializer extends JsonSerializer<Vault> {

	@Override
	public void serialize(Vault value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeStringField("path", value.getPath().toString());
		jgen.writeStringField("mountName", value.getMountName().toString());
		jgen.writeEndObject();
	}

}
