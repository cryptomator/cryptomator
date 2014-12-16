/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel, Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation, strategy fine tuning
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.ui.util.mount;

import static org.cryptomator.ui.util.command.Script.fromLines;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.command.CommandResult;
import org.cryptomator.ui.util.command.Script;

/**
 * A {@link WebDavMounterStrategy} utilizing the "net use" command.
 * <p>
 * Tested on Windows 7 but should also work on Windows 8.
 *
 * @author Markus Kreusch
 */
final class WindowsWebDavMounter implements WebDavMounterStrategy {

	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*([A-Z]:)\\s*");
	
	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public WebDavMount mount(int localPort) throws CommandFailedException {
		final Script mountScript = fromLines("net use * http://0--1.ipv6-literal.net:%PORT% /persistent:no")
				.addEnv("PORT", String.valueOf(localPort));
		mountScript.setTimeout(30);
		mountScript.setTimeoutUnit(TimeUnit.SECONDS);
		final CommandResult mountResult = mountScript.execute();
		final String driveLetter = getDriveLetter(mountResult.getOutput());
		final Script unmountScript = fromLines("net use "+driveLetter+" /delete")
				.addEnv("DRIVE_LETTER", driveLetter);
		return new WebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				unmountScript.execute();
			}
		};
	}

	private String getDriveLetter(String result) throws CommandFailedException {
		final Matcher matcher = WIN_MOUNT_DRIVELETTER_PATTERN.matcher(result);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new CommandFailedException("Failed to get a drive letter from net use output.");
		}
	}

}
