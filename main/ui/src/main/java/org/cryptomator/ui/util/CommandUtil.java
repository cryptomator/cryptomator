/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel, Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util;

import static java.lang.String.join;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.cryptomator.ui.util.webdav.CommandFailedException;

public final class CommandUtil {

	private static final int DEFAULT_TIMEOUT_SECONDS = 3;
	
	public static String exec(String ... command) throws CommandFailedException {
		try {
			final Process proc = Runtime.getRuntime().exec(command);
			if (!proc.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				proc.destroy();
				throw new CommandFailedException("Timeout executing command " + join(" ", command));
			}
			if (proc.exitValue() != 0) {
				throw new CommandFailedException(IOUtils.toString(proc.getErrorStream()));
			}
			return IOUtils.toString(proc.getInputStream());
		} catch (IOException | InterruptedException | IllegalThreadStateException e) {
			throw new CommandFailedException(e);
		}
	}
	
	private CommandUtil() {
		throw new IllegalStateException("Class is not instantiable");
	}
	
}
