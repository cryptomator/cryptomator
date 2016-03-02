/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel, Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation, strategy fine tuning
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend.MountParam;

@Singleton
final class MacOsXWebDavMounter implements WebDavMounterStrategy {

	@Inject
	MacOsXWebDavMounter() {
	}

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_MAC_OSX;
	}

	@Override
	public void warmUp(int serverPort) {
		// no-op
	}

	@Override
	public WebDavMount mount(URI uri, Map<MountParam, Optional<String>> mountParams) throws CommandFailedException {
		try {
			String mountAppleScript = String.format("mount volume \"%s\"", uri.toString());
			ProcessBuilder mount = new ProcessBuilder("/usr/bin/osascript", "-e", mountAppleScript);
			Process mountProcess = mount.start();
			String stdout = IOUtils.toString(mountProcess.getInputStream(), StandardCharsets.UTF_8);
			waitForProcessAndCheckSuccess(mountProcess, 1, TimeUnit.SECONDS);
			String volumeIdentifier = StringUtils.trim(StringUtils.removeStart(stdout, "file "));
			String waitAppleScript1 = String.format("tell application \"Finder\" to repeat while not (\"%s\" exists)", volumeIdentifier);
			String waitAppleScript2 = "delay 0.1";
			String waitAppleScript3 = "end repeat";
			ProcessBuilder wait = new ProcessBuilder("/usr/bin/osascript", "-e", waitAppleScript1, "-e", waitAppleScript2, "-e", waitAppleScript3);
			Process waitProcess = wait.start();
			waitForProcessAndCheckSuccess(waitProcess, 5, TimeUnit.SECONDS);
			return new MacWebDavMount(volumeIdentifier);
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private static class MacWebDavMount extends AbstractWebDavMount {
		private final ProcessBuilder revealCommand;
		private final ProcessBuilder unmountCommand;

		private MacWebDavMount(String volumeIdentifier) {
			String openAppleScript = String.format("tell application \"Finder\" to open \"%s\"", volumeIdentifier);
			String activateAppleScript = String.format("tell application \"Finder\" to activate \"%s\"", volumeIdentifier);
			String ejectAppleScript = String.format("tell application \"Finder\" to if \"%s\" exists then eject \"%s\"", volumeIdentifier, volumeIdentifier);

			this.revealCommand = new ProcessBuilder("/usr/bin/osascript", "-e", openAppleScript, "-e", activateAppleScript);
			this.unmountCommand = new ProcessBuilder("/usr/bin/osascript", "-e", ejectAppleScript);
		}

		@Override
		public void unmount() throws CommandFailedException {
			try {
				Process proc = unmountCommand.start();
				waitForProcessAndCheckSuccess(proc, 1, TimeUnit.SECONDS);
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			try {
				Process proc = revealCommand.start();
				waitForProcessAndCheckSuccess(proc, 2, TimeUnit.SECONDS);
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

	}

	private static void waitForProcessAndCheckSuccess(Process proc, long timeout, TimeUnit unit) throws CommandFailedException, IOException {
		try {
			boolean finishedInTime = proc.waitFor(timeout, unit);
			if (!finishedInTime) {
				proc.destroyForcibly();
				throw new CommandFailedException("Command timed out.");
			}
			int exitCode = proc.exitValue();
			if (exitCode != 0) {
				String error = IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8);
				throw new CommandFailedException("Command failed with exit code " + exitCode + ". Stderr: " + error);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
