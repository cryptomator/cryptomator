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
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.webdav.CommandFailedException;

/**
 * <p>
 * Runs commands using a system compatible CLI.
 * <p>
 * To detect the system type {@link SystemUtils} is used. The following CLIs are
 * used by default:
 * <ul>
 * <li><i>{@link #WINDOWS_DEFAULT_CLI}</i> if {@link SystemUtils#IS_OS_WINDOWS}
 * <li><i>{@link #UNIX_DEFAULT_CLI}</i> if {@link SystemUtils#IS_OS_UNIX}
 * </ul>
 * <p>
 * If the path to the executables differs from the default or the system can not
 * be detected the Java system property {@value #CLI_EXECUTABLE_PROPERTY} can be
 * set to define it.
 * <p>
 * If a CLI executable can not be determined using these methods operation of
 * {@link CommandRunner} will fail with {@link IllegalStateException}s.
 *
 * @author Markus Kreusch
 */
class CommandRunner {

	public static final String CLI_EXECUTABLE_PROPERTY = "cryptomator.cli";
	
	public static final String WINDOWS_DEFAULT_CLI[] = {"cmd"};
	public static final String UNIX_DEFAULT_CLI[] = {"/bin/sh", "-e"};

	static CommandResult execute(Script script) throws CommandFailedException {
		ProcessBuilder builder = new ProcessBuilder(determineCli());
		builder.environment().clear();
		builder.environment().putAll(script.environment());
		try {
			return run(builder.start(), script.getLines());
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private static CommandResult run(Process process, String[] lines) {
		return new CommandResult(process, lines);
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
			throw new IllegalStateException(format(
					"Failed to determine cli to use. Set Java system property %s to the executable.",
					CLI_EXECUTABLE_PROPERTY));
		}
	}
	
}
