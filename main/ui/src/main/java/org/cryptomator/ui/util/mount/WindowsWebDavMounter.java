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
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
 */
final class WindowsWebDavMounter implements WebDavMounterStrategy {

	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*([A-Z]:)\\s*");
	private static final int MAX_MOUNT_ATTEMPTS = 5;

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public void warmUp(int serverPort) {
//		try {
//			final Script mountScript = fromLines("net use * \\\\localhost@%DAV_PORT%\\DavWWWRoot\\bill-gates-mom-uses-goto /persistent:no");
//			mountScript.addEnv("DAV_PORT", String.valueOf(serverPort));
//			mountScript.execute(1, TimeUnit.SECONDS);
//		} catch (CommandFailedException e) {
//            // will most certainly throw an exception, because this is a fake WebDav path. But now windows has some DNS things cached :)
//		}
	}

	@Override
	public WebDavMount mount(URI uri, String name) throws CommandFailedException {
		CommandResult mountResult;
		try {
			final Script mountScript = fromLines("net use * \\\\0--1.ipv6-literal.net@%DAV_PORT%\\DavWWWRoot%DAV_UNC_PATH% /persistent:no");
			mountScript.addEnv("DAV_PORT", String.valueOf(uri.getPort())).addEnv("DAV_UNC_PATH", uri.getRawPath().replace('/', '\\'));
			mountResult = mountScript.execute(5, TimeUnit.SECONDS);
		} catch (CommandFailedException ex) {
			final Script mountScript = fromLines("net use * \\\\0--1.ipv6-literal.net@%DAV_PORT%\\DavWWWRoot%DAV_UNC_PATH% /persistent:no");
			mountScript.addEnv("DAV_PORT", String.valueOf(uri.getPort())).addEnv("DAV_UNC_PATH", uri.getRawPath().replace('/', '\\'));
			final Script proxyBypassScript = fromLines("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v \"ProxyOverride\" /d \"<local>;0--1.ipv6-literal.net;0--1.ipv6-literal.net:%DAV_PORT%\" /f");
	        proxyBypassScript.addEnv("DAV_PORT", String.valueOf(uri.getPort()));
			mountResult = bypassProxyAndRetryMount(mountScript, proxyBypassScript);
		}
		
		final String driveLetter = getDriveLetter(mountResult.getStdOut());
		final Script openExplorerScript = fromLines("start explorer.exe " + driveLetter);
		openExplorerScript.execute();
		final Script unmountScript = fromLines("net use " + driveLetter + " /delete").addEnv("DRIVE_LETTER", driveLetter);
		return new AbstractWebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				// only attempt unmount if user didn't unmount manually:
				if (isVolumeMounted(driveLetter)) {
					unmountScript.execute();
				}
			}
		};
	}
	
	private boolean isVolumeMounted(String driveLetter) {
		for (Path path : FileSystems.getDefault().getRootDirectories()) {
			if (path.toString().startsWith(driveLetter)) {
				return true;
			}
		}
		return false;
	}
	
	private CommandResult bypassProxyAndRetryMount(Script mountScript, Script proxyBypassScript) throws CommandFailedException {
		CommandFailedException latestException = null;
		for (int i = 0; i < MAX_MOUNT_ATTEMPTS; i++) {
			try {
				// wait a moment before next attempt
				Thread.sleep(5000);
				proxyBypassScript.execute();
				return mountScript.execute(5, TimeUnit.SECONDS);
			} catch (CommandFailedException ex) {
				latestException = ex;
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new CommandFailedException(ex);
			}
		}
		throw latestException;
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
