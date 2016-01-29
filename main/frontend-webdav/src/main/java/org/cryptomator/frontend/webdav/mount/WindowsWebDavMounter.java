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

import static org.cryptomator.frontend.webdav.mount.command.Script.fromLines;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.frontend.CommandFailedException;
import org.cryptomator.frontend.Frontend.MountParam;
import org.cryptomator.frontend.webdav.mount.command.CommandResult;
import org.cryptomator.frontend.webdav.mount.command.Script;

/**
 * A {@link WebDavMounterStrategy} utilizing the "net use" command.
 * <p>
 * Tested on Windows 7 but should also work on Windows 8.
 */
@Singleton
final class WindowsWebDavMounter implements WebDavMounterStrategy {

	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*([A-Z]):\\s*");
	private static final int MAX_MOUNT_ATTEMPTS = 8;
	private static final char AUTO_ASSIGN_DRIVE_LETTER = '*';
	private final WindowsDriveLetters driveLetters;

	@Inject
	WindowsWebDavMounter(WindowsDriveLetters driveLetters) {
		this.driveLetters = driveLetters;
	}

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public void warmUp(int serverPort) {
		// no-op
	}

	@Override
	public WebDavMount mount(URI uri, Map<MountParam, Optional<String>> mountParams) throws CommandFailedException {
		final Character driveLetter = mountParams.get(MountParam.WIN_DRIVE_LETTER).map(CharUtils::toCharacterObject).orElse(AUTO_ASSIGN_DRIVE_LETTER);
		if (driveLetters.getOccupiedDriveLetters().contains(driveLetter)) {
			throw new CommandFailedException("Drive letter occupied.");
		}

		final String driveLetterStr = driveLetter.charValue() == AUTO_ASSIGN_DRIVE_LETTER ? CharUtils.toString(AUTO_ASSIGN_DRIVE_LETTER) : driveLetter + ":";
		final Script localhostMountScript = fromLines("net use %DRIVE_LETTER% \\\\localhost@%DAV_PORT%\\DavWWWRoot%DAV_UNC_PATH% /persistent:no");
		localhostMountScript.addEnv("DRIVE_LETTER", driveLetterStr);
		localhostMountScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
		localhostMountScript.addEnv("DAV_UNC_PATH", uri.getRawPath().replace('/', '\\'));
		CommandResult mountResult;
		try {
			mountResult = localhostMountScript.execute(5, TimeUnit.SECONDS);
		} catch (CommandFailedException ex) {
			final Script ipv6literaltMountScript = fromLines("net use %DRIVE_LETTER% \\\\0--1.ipv6-literal.net@%DAV_PORT%\\DavWWWRoot%DAV_UNC_PATH% /persistent:no");
			ipv6literaltMountScript.addEnv("DRIVE_LETTER", driveLetterStr);
			ipv6literaltMountScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
			ipv6literaltMountScript.addEnv("DAV_UNC_PATH", uri.getRawPath().replace('/', '\\'));
			final Script proxyBypassScript = fromLines(
					"reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v \"ProxyOverride\" /d \"<local>;0--1.ipv6-literal.net;0--1.ipv6-literal.net:%DAV_PORT%\" /f");
			proxyBypassScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
			mountResult = bypassProxyAndRetryMount(localhostMountScript, ipv6literaltMountScript, proxyBypassScript);
		}
		return new WindowsWebDavMount(driveLetter.charValue() == AUTO_ASSIGN_DRIVE_LETTER ? getDriveLetter(mountResult.getStdOut()) : driveLetter);
	}

	private CommandResult bypassProxyAndRetryMount(Script localhostMountScript, Script ipv6literalMountScript, Script proxyBypassScript) throws CommandFailedException {
		CommandFailedException latestException = null;
		for (int i = 0; i < MAX_MOUNT_ATTEMPTS; i++) {
			try {
				// wait a moment before next attempt
				Thread.sleep(5000);
				proxyBypassScript.execute();
				// alternate localhost and 0--1.ipv6literal.net
				final Script mountScript = (i % 2 == 0) ? localhostMountScript : ipv6literalMountScript;
				return mountScript.execute(3, TimeUnit.SECONDS);
			} catch (CommandFailedException ex) {
				latestException = ex;
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new CommandFailedException(ex);
			}
		}
		throw latestException;
	}

	private Character getDriveLetter(String result) throws CommandFailedException {
		final Matcher matcher = WIN_MOUNT_DRIVELETTER_PATTERN.matcher(result);
		if (matcher.find()) {
			return CharUtils.toCharacterObject(matcher.group(1));
		} else {
			throw new CommandFailedException("Failed to get a drive letter from net use output.");
		}
	}

	private class WindowsWebDavMount extends AbstractWebDavMount {
		private final Character driveLetter;
		private final Script openExplorerScript;
		private final Script unmountScript;

		private WindowsWebDavMount(Character driveLetter) {
			this.driveLetter = driveLetter;
			this.openExplorerScript = fromLines("start explorer.exe " + driveLetter + ":");
			this.unmountScript = fromLines("net use " + driveLetter + ": /delete").addEnv("DRIVE_LETTER", Character.toString(driveLetter));
		}

		@Override
		public void unmount() throws CommandFailedException {
			// only attempt unmount if user didn't unmount manually:
			if (isVolumeMounted()) {
				unmountScript.execute();
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			openExplorerScript.execute();
		}

		private boolean isVolumeMounted() {
			return driveLetters.getOccupiedDriveLetters().contains(driveLetter);
		}
	}

}
