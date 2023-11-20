package org.cryptomator.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.InvalidPropertiesFormatException;

// New class for handling XML-related functionality, adding this class to resolve the insufficient modularization smell
public  class XmlHandler {
	void storeToXML(PropertiesDecorator properties, OutputStream os, String comment) throws IOException {
		properties.delegate.storeToXML(os, comment);
	}

	void storeToXML(PropertiesDecorator properties, OutputStream os, String comment, String encoding) throws IOException {
		properties.delegate.storeToXML(os, comment, encoding);
	}

	void storeToXML(PropertiesDecorator properties, OutputStream os, String comment, Charset charset) throws IOException {
		properties.delegate.storeToXML(os, comment, charset);
	}

	void loadFromXML(PropertiesDecorator properties, InputStream in) throws IOException, InvalidPropertiesFormatException {
		properties.delegate.loadFromXML(in);
	}
}