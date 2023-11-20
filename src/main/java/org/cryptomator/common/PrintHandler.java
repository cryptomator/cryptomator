package org.cryptomator.common;

import java.io.PrintStream;
import java.io.PrintWriter;

// New class for handling printing functionality
public class PrintHandler {
	void list(PropertiesDecorator properties, PrintStream out) {
		properties.delegate.list(out);
	}

	void list(PropertiesDecorator properties, PrintWriter out) {
		properties.delegate.list(out);
	}
}