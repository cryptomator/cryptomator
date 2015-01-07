package org.cryptomator.ui.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DirectorySerializer extends JsonSerializer<Directory> {

	@Override
	public void serialize(Directory value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeStringField("path", value.getPath().toString());
		jgen.writeBooleanField("checkIntegrity", value.shouldVerifyFileIntegrity());
		jgen.writeEndObject();
	}

}
