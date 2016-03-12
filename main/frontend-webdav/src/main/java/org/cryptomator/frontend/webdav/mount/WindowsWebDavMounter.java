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
import java.net.URISyntaxException;
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
 * Tested on Windows 7, 8.1 and 10.
 */
@Singleton
final class WindowsWebDavMounter implements WebDavMounterStrategy {

	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*([A-Z]):\\s*");
	private static final String AUTO_ASSIGN_DRIVE_LETTER = "*";
	private static final String LOCALHOST = "localhost";
	private static final int MOUNT_TIMEOUT_SECONDS = 60;
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
		final String driveLetter = mountParams.getOrDefault(MountParam.WIN_DRIVE_LETTER, Optional.of(AUTO_ASSIGN_DRIVE_LETTER)).orElse(AUTO_ASSIGN_DRIVE_LETTER);
		if (driveLetters.getOccupiedDriveLetters().contains(CharUtils.toChar(driveLetter))) {
			throw new CommandFailedException("Drive letter occupied.");
		}
		
		final String hostname = mountParams.getOrDefault(MountParam.HOSTNAME, Optional.of(LOCALHOST)).orElse(LOCALHOST);
		try {
			final URI adjustedUri = new URI(uri.getScheme(), uri.getUserInfo(), hostname, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
			CommandResult mountResult = mount(adjustedUri, driveLetter);
			return new WindowsWebDavMount(AUTO_ASSIGN_DRIVE_LETTER.equals(driveLetter) ? getDriveLetter(mountResult.getStdOut()) : driveLetter);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid host: " + hostname);
		}
	}
	
	private CommandResult mount(URI uri, String driveLetter) throws CommandFailedException {
		final Script proxyBypassScript = fromLines(
				"reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v \"ProxyOverride\" /d \"<local>;%DAV_HOST%;%DAV_HOST%:%DAV_PORT%\" /f");
		proxyBypassScript.addEnv("DAV_HOST", uri.getHost());
		proxyBypassScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
		proxyBypassScript.execute();
		
		final String driveLetterStr = AUTO_ASSIGN_DRIVE_LETTER.equals(driveLetter) ? AUTO_ASSIGN_DRIVE_LETTER : driveLetter + ":";
		final Script mountScript = fromLines("net use %DRIVE_LETTER% \\\\%DAV_HOST%@%DAV_PORT%\\DavWWWRoot%DAV_UNC_PATH% /persistent:no");
		mountScript.addEnv("DRIVE_LETTER", driveLetterStr);
		mountScript.addEnv("DAV_HOST", uri.getHost());
		mountScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
		mountScript.addEnv("DAV_UNC_PATH", uri.getRawPath().replace('/', '\\'));
		return mountScript.execute(MOUNT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	private String getDriveLetter(String result) throws CommandFailedException {
		final Matcher matcher = WIN_MOUNT_DRIVELETTER_PATTERN.matcher(result);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new CommandFailedException("Failed to get a drive letter from net use output.");
		}
	}

	private class WindowsWebDavMount extends AbstractWebDavMount {
		private final Character driveLetter;
		private final Script openExplorerScript;
		private final Script unmountScript;

		private WindowsWebDavMount(String driveLetter) {
			this.driveLetter = CharUtils.toCharacterObject(driveLetter);
			this.openExplorerScript = fromLines("start explorer.exe " + driveLetter + ":");
			this.unmountScript = fromLines("net use " + driveLetter + ": /delete");
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
