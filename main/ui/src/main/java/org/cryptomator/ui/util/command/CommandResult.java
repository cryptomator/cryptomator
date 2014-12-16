/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 *     Sebastian Stenzel - using Futures, lazy loading for out/err.
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import static java.lang.String.format;
import static org.apache.commons.io.IOUtils.copy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cryptomator.ui.util.mount.CommandFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandResult {

	private static final Logger LOG = LoggerFactory.getLogger(CommandResult.class);

	private final Process process;

	public CommandResult(Process process) {
		this.process = process;		
	}

	public String getOutput() throws CommandFailedException {
		try (InputStream in = process.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			copy(in, out);
			return new String(out.toByteArray());
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	public String getError() throws CommandFailedException {
		try (InputStream in = process.getErrorStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			copy(in, out);
			return new String(out.toByteArray());
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	public int getExitValue() throws CommandFailedException {
		return process.exitValue();
	}

	public void logDebugInfo() {
		if (LOG.isDebugEnabled()) {
			try {
				LOG.debug("Command execution finished. Exit code: {}\n" + "Output:\n" + "{}\n" + "Error:\n" + "{}\n", process.exitValue(), getOutput(), getError());
			} catch (CommandFailedException e) {
				LOG.debug("Command execution finished. Exit code: {}\n", process.exitValue());
			}
		}
	}

	void assertOk() throws CommandFailedException {
		int exitValue = getExitValue();
		if (exitValue != 0) {
			throw new CommandFailedException(format("Command execution failed. Exit code: %d\n" + "# Output:\n" + "%s\n" + "# Error:\n" + "%s", exitValue, getOutput(), getError()));
		}
	}

}
