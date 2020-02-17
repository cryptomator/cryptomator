package org.cryptomator.ui.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class StackTraceController implements FxController {

	private final String stackTrace;

	public StackTraceController(Throwable cause) {
		this.stackTrace = provideStackTrace(cause);
	}

	private static String provideStackTrace(Throwable cause) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cause.printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	/* Getter/Setter */

	public String getStackTrace() {
		return stackTrace;
	}


}
