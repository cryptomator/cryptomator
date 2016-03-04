/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 *     Sebastian Stenzel - using Futures, lazy loading for out/err.
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount.command;

import static java.lang.String.format;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.cryptomator.frontend.CommandFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandResult {

	private static final Logger LOG = LoggerFactory.getLogger(CommandResult.class);

	private final Process process;
	private final String stdout;
	private final String stderr;
	private final CommandFailedException exception;

	/**
	 * Constructs a CommandResult from a terminated process and closes all its streams.
	 * 
	 * @param process An <strong>already finished</strong> process.
	 */
	CommandResult(Process process) {
		String out = null;
		String err = null;
		CommandFailedException ex = null;
		try {
			out = IOUtils.toString(process.getInputStream());
			err = IOUtils.toString(process.getErrorStream());
		} catch (IOException e) {
			ex = new CommandFailedException(e);
		} finally {
			this.process = process;
			this.stdout = out;
			this.stderr = err;
			this.exception = ex;
			IOUtils.closeQuietly(process.getInputStream());
			IOUtils.closeQuietly(process.getOutputStream());
			IOUtils.closeQuietly(process.getErrorStream());
			logDebugInfo();
		}
	}

	/**
	 * @return Data written to STDOUT
	 */
	public String getStdOut() throws CommandFailedException {
		assertNoException();
		return stdout;
	}

	/**
	 * @return Data written to STDERR
	 */
	public String getStdErr() throws CommandFailedException {
		assertNoException();
		return stderr;
	}

	/**
	 * @return Exit value of the process
	 */
	public int getExitValue() {
		return process.exitValue();
	}

	private void logDebugInfo() {
		if (LOG.isDebugEnabled()) {
			if (Strings.isEmpty(stderr) && Strings.isEmpty(stdout)) {
				LOG.debug("Command execution finished. Exit code: {}", process.exitValue());
			} else if (Strings.isEmpty(stderr)) {
				LOG.debug("Command execution finished. Exit code: {}\nOutput: {}", process.exitValue(), stdout);
			} else if (Strings.isEmpty(stdout)) {
				LOG.debug("Command execution finished. Exit code: {}\nError: {}", process.exitValue(), stderr);
			} else {
				LOG.debug("Command execution finished. Exit code: {}\n Output: {}\nError: {}", process.exitValue(), stdout, stderr);
			}
		}
	}

	void assertOk() throws CommandFailedException {
		assertNoException();
		int exitValue = getExitValue();
		if (exitValue != 0) {
			throw new CommandFailedException(format("Command execution failed. Exit code: %d %n# Output:%n%s %n# Error: %n%s", exitValue, stdout, stderr));
		}
	}

	private void assertNoException() throws CommandFailedException {
		if (exception != null) {
			throw exception;
		}
	}

}
