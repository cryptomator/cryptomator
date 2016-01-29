/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 *     Sebastian Stenzel - Refactoring
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount.command;

import static java.lang.String.format;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.frontend.CommandFailedException;

/**
 * <p>
 * Runs commands using a system compatible CLI.
 * <p>
 * To detect the system type {@link SystemUtils} is used. The following CLIs are used by default:
 * <ul>
 * <li><i>{@link #WINDOWS_DEFAULT_CLI}</i> if {@link SystemUtils#IS_OS_WINDOWS}
 * <li><i>{@link #UNIX_DEFAULT_CLI}</i> if {@link SystemUtils#IS_OS_UNIX}
 * </ul>
 * <p>
 * If the path to the executables differs from the default or the system can not be detected the Java system property
 * {@value #CLI_EXECUTABLE_PROPERTY} can be set to define it.
 * <p>
 * If a CLI executable can not be determined using these methods operation of {@link CommandRunner} will fail with
 * {@link IllegalStateException}s.
 *
 * @author Markus Kreusch
 */
final class CommandRunner {

	public static final String CLI_EXECUTABLE_PROPERTY = "cryptomator.cli";
	public static final String WINDOWS_DEFAULT_CLI[] = {"cmd", "/C"};
	public static final String UNIX_DEFAULT_CLI[] = {"/bin/sh", "-c"};
	private static final Executor CMD_EXECUTOR = Executors.newCachedThreadPool();

	/**
	 * Executes all lines in the given script in the specified order. Stops as soon as the first command fails.
	 * 
	 * @param script Script containing command lines and environment variables.
	 * @return Result of the last command, if it exited successfully.
	 * @throws CommandFailedException If one of the command lines in the given script fails.
	 */
	static CommandResult execute(Script script, long timeout, TimeUnit unit) throws CommandFailedException {
		try {
			final List<String> env = script.environment().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
			CommandResult result = null;
			for (final String line : script.getLines()) {
				final String[] cmds = ArrayUtils.add(determineCli(), line);
				final Process proc = Runtime.getRuntime().exec(cmds, env.toArray(new String[0]));
				result = run(proc, timeout, unit);
				result.assertOk();
			}
			return result;
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private static CommandResult run(Process process, long timeout, TimeUnit unit) throws CommandFailedException {
		try {
			final FutureCommandResult futureCommandResult = new FutureCommandResult(process);
			CMD_EXECUTOR.execute(futureCommandResult);
			return futureCommandResult.get(timeout, unit);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new CommandFailedException("Waiting time elapsed before command execution finished");
		}
	}

	private static String[] determineCli() {
		final String cliFromProperty = System.getProperty(CLI_EXECUTABLE_PROPERTY);
		if (cliFromProperty != null) {
			return cliFromProperty.split("");
		} else if (IS_OS_WINDOWS) {
			return WINDOWS_DEFAULT_CLI;
		} else if (IS_OS_UNIX) {
			return UNIX_DEFAULT_CLI;
		} else {
			throw new IllegalStateException(format("Failed to determine cli to use. Set Java system property %s to the executable.", CLI_EXECUTABLE_PROPERTY));
		}
	}

}
