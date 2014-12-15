/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.cryptomator.ui.util.mount.CommandFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandResult {
	
	private static final int DEFAULT_TIMEOUT_MILLISECONDS = 10000;
	
	private static final Logger LOG = LoggerFactory.getLogger(CommandResult.class);

	private final ByteArrayOutputStream output = new ByteArrayOutputStream();
	private final ByteArrayOutputStream error = new ByteArrayOutputStream();
	private final Process process;
	
	private final AsyncStreamCopier processOutputCopier;
	private final AsyncStreamCopier processErrorCopier;
	
	private boolean finished;
	
	public CommandResult(Process process, String[] lines) {
		this.process = process;
		new AsyncLineWriter(lines, process.getOutputStream());
		processOutputCopier = new AsyncStreamCopier(process.getInputStream(), output);
		processErrorCopier = new AsyncStreamCopier(process.getErrorStream(), error);
	}
	
	public String getOutput() throws CommandFailedException {
		return getOutput(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MICROSECONDS);
	}
	
	public String getError() throws CommandFailedException {
		return getError(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MICROSECONDS);
	}
	
	public String getOutput(long timeout, TimeUnit unit) throws CommandFailedException {
		waitAndAssertOk(timeout, unit);
		return new String(output.toByteArray());
	}
	
	public String getError(long timeout, TimeUnit unit) throws CommandFailedException {
		waitAndAssertOk(timeout, unit);
		return new String(error.toByteArray());
	}
	
	public int getExitValue(long timeout, TimeUnit unit) throws CommandFailedException {
		waitAndAssertOk(timeout, unit);
		return process.exitValue();
	}

	private void waitAndAssertOk(long timeout, TimeUnit unit) throws CommandFailedException {
		if (finished) return;
		try {
			if (!process.waitFor(timeout, unit)) {
				throw new CommandFailedException("Waiting time elapsed before command execution finished");
			}
			processOutputCopier.assertOk();
			processErrorCopier.assertOk();
			finished = true;
			logDebugInfo();
		} catch (IOException | InterruptedException e) {
			throw new CommandFailedException(e);
		}
	}

	private void logDebugInfo() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Command execution finished. Exit code: {}\n"
					+ "Output:\n"
					+ "{}\n"
					+ "Error:\n"
					+ "{}\n",
					process.exitValue(),
					new String(output.toByteArray()),
					new String(error.toByteArray()));
		}
	}

	public void assertOk() throws CommandFailedException {
		assertOk(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
	}
	
	public void assertOk(long timeout, TimeUnit unit) throws CommandFailedException {
		int exitValue = getExitValue(timeout, unit);
		if (exitValue != 0) {
			throw new CommandFailedException(format(
					"Command execution failed. Exit code: %d\n"
					+ "# Output:\n"
					+ "%s\n"
					+ "# Error:\n"
					+ "%s",
					exitValue,
					new String(output.toByteArray()),
					new String(error.toByteArray())));
		}
	}

}
