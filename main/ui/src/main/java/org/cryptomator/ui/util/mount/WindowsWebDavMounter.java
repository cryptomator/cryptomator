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

import java.net.URI;
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
	public void warmUp(int serverPort) {
		try {
			final Script proxyBypassCmd = fromLines("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v \"ProxyOverride\" /d \"<local>;0--1.ipv6-literal.net\" /f");
			proxyBypassCmd.execute();
			final Script mountCmd = fromLines("net use * http://0--1.ipv6-literal.net:" + serverPort + "/bill-gates-mom-uses-goto /persistent:no");
			mountCmd.execute();
		} catch (CommandFailedException e) {
			// will most certainly throw an exception, because this is a fake WebDav path. But now windows has some DNS things cached :)
		}
	}

	@Override
	public WebDavMount mount(URI uri, String name) throws CommandFailedException {
		final Script mountScript = fromLines("net use * http://0--1.ipv6-literal.net:%PORT%%DAV_PATH% /persistent:no")
				.addEnv("PORT", String.valueOf(uri.getPort()))
				.addEnv("DAV_PATH", uri.getRawPath());
		final CommandResult mountResult = mountScript.execute(30, TimeUnit.SECONDS);
		final String driveLetter = getDriveLetter(mountResult.getStdOut());
		final Script openExplorerScript = fromLines("start explorer.exe " + driveLetter);
		openExplorerScript.execute();
		final Script unmountScript = fromLines("net use " + driveLetter + " /delete").addEnv("DRIVE_LETTER", driveLetter);
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
