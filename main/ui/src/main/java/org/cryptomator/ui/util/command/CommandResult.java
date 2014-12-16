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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cryptomator.ui.util.mount.CommandFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandResult {

	private static final Logger LOG = LoggerFactory.getLogger(CommandResult.class);

	private final ByteArrayOutputStream output = new ByteArrayOutputStream();
	private final ByteArrayOutputStream error = new ByteArrayOutputStream();
	private final Lock lock = new ReentrantLock();
	private final Condition finishedCondition = lock.newCondition();
	private final Process process;

	private final AsyncStreamCopier processOutputCopier;
	private final AsyncStreamCopier processErrorCopier;

	private boolean finished;
	private CommandFailedException exception;

	public CommandResult(Process process, String[] lines, Executor cmdExecutor, long timeout, TimeUnit unit) {
		this.process = process;
		processOutputCopier = new AsyncStreamCopier(process.getInputStream(), output);
		processErrorCopier = new AsyncStreamCopier(process.getErrorStream(), error);
		cmdExecutor.execute(processOutputCopier);
		cmdExecutor.execute(processErrorCopier);
		cmdExecutor.execute(new CommandTask(timeout, unit));
	}

	public String getOutput() throws CommandFailedException {
		this.waitUntilFinished();
		return new String(output.toByteArray());
	}

	public String getError() throws CommandFailedException {
		this.waitUntilFinished();
		return new String(error.toByteArray());
	}

	public int getExitValue() throws CommandFailedException {
		this.waitUntilFinished();
		return process.exitValue();
	}

	private void waitUntilFinished() throws CommandFailedException {
		lock.lock();
		try {
			while (!finished) {
				finishedCondition.await();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
		if (exception != null) {
			throw exception;
		}
	}

	private void logDebugInfo() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Command execution finished. Exit code: {}\n" + "Output:\n" + "{}\n" + "Error:\n" + "{}\n", process.exitValue(), new String(output.toByteArray()), new String(error.toByteArray()));
		}
	}

	void assertOk() throws CommandFailedException {
		int exitValue = getExitValue();
		if (exitValue != 0) {
			throw new CommandFailedException(format("Command execution failed. Exit code: %d\n" + "# Output:\n" + "%s\n" + "# Error:\n" + "%s", exitValue, new String(output.toByteArray()),
					new String(error.toByteArray())));
		}
	}

	private class CommandTask implements Runnable {

		private final long timeout;
		private final TimeUnit unit;

		private CommandTask(long timeout, TimeUnit unit) {
			this.timeout = timeout;
			this.unit = unit;
		}

		@Override
		public void run() {
			try {
				if (!process.waitFor(timeout, unit)) {
					exception = new CommandFailedException("Waiting time elapsed before command execution finished");
				}
				processOutputCopier.assertOk();
				processErrorCopier.assertOk();
				logDebugInfo();
			} catch (IOException | InterruptedException e) {
				exception = new CommandFailedException(e);
			} finally {
				lock.lock();
				finished = true;
				finishedCondition.signal();
				lock.unlock();
			}
		}
	}

}
