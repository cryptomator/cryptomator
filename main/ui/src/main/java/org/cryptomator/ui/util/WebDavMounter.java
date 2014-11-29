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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebDavMounter {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounter.class);
	private static final int CMD_DEFAULT_TIMEOUT = 1;

	private WebDavMounter() {
		throw new IllegalStateException("not instantiable.");
	}

	public static void mount(int localPort) throws CommandFailedException {
		if (SystemUtils.IS_OS_MAC_OSX) {
			exec("mkdir /Volumes/Cryptomator", CMD_DEFAULT_TIMEOUT);
			exec("mount_webdav -S -v Cryptomator localhost:" + localPort + " /Volumes/Cryptomator", CMD_DEFAULT_TIMEOUT);
			exec("open /Volumes/Cryptomator", CMD_DEFAULT_TIMEOUT);
		}
	}

	public static void unmount(int timeout) throws CommandFailedException {
		if (SystemUtils.IS_OS_MAC_OSX) {
			exec("umount /Volumes/Cryptomator", timeout);
		}
	}

	private static void exec(String cmd, int timoutSeconds) throws CommandFailedException {
		try {
			final Process proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
			if (proc.waitFor(timoutSeconds, TimeUnit.SECONDS)) {
				proc.destroy();
			}
			if (proc.exitValue() != 0) {
				throw new CommandFailedException(IOUtils.toString(proc.getErrorStream()));
			}
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
