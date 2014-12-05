/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebDavMounter {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounter.class);
	private static final int CMD_DEFAULT_TIMEOUT = 3;
	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*[A-Z]:\\s*");

	private WebDavMounter() {
		throw new IllegalStateException("not instantiable.");
	}
	
	/**
	 * @return Unmount Command
	 */
	public static synchronized String mount(int localPort) throws CommandFailedException {
		if (SystemUtils.IS_OS_MAC_OSX) {
			exec("mkdir /Volumes/Cryptomator" + localPort, CMD_DEFAULT_TIMEOUT);
			exec("mount_webdav -S -v Cryptomator localhost:" + localPort + " /Volumes/Cryptomator" + localPort, CMD_DEFAULT_TIMEOUT);
			exec("open /Volumes/Cryptomator" + localPort, CMD_DEFAULT_TIMEOUT);
			return "umount /Volumes/Cryptomator" + localPort;
		} else if (SystemUtils.IS_OS_WINDOWS) {
			final String result = exec("net use * http://127.0.0.1:" + localPort + " /persistent:no", CMD_DEFAULT_TIMEOUT);
			final Matcher matcher = WIN_MOUNT_DRIVELETTER_PATTERN.matcher(result);
			if (matcher.find()) {
				final String driveLetter = matcher.group();
				return "net use " + driveLetter + " /delete";
			}
		}
		return null;
	}

	public static void unmount(String command) throws CommandFailedException {
		if (command != null) {
			exec(command, CMD_DEFAULT_TIMEOUT);
		}
	}

	private static String exec(String cmd, int timoutSeconds) throws CommandFailedException {
		try {
			final Process proc;
			if (SystemUtils.IS_OS_WINDOWS) {
				proc = Runtime.getRuntime().exec(new String[]{"cmd", "/C", cmd});
			} else {
				proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
			}
			if (proc.waitFor(timoutSeconds, TimeUnit.SECONDS)) {
				proc.destroy();
			}
			if (proc.exitValue() != 0) {
				throw new CommandFailedException(IOUtils.toString(proc.getErrorStream()));
			}
			return  IOUtils.toString(proc.getInputStream());
		} catch (IOException | InterruptedException | IllegalThreadStateException e) {
			LOG.error("Command execution failed.", e);
			throw new CommandFailedException(e);
		}

	}

	public static class CommandFailedException extends Exception {

		private static final long serialVersionUID = 5784853630182321479L;

		private CommandFailedException(String message) {
			super(message);
		}

		private CommandFailedException(Throwable cause) {
			super(cause);
		}

	}

}
