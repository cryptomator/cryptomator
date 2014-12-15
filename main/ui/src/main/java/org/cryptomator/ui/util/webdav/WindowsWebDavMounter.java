/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel, Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.ui.util.webdav;

import static java.lang.String.format;
import static org.cryptomator.ui.util.command.Script.fromLines;

import java.net.URI;
import java.net.URISyntaxException;
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

	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("Laufwerk\\s*([A-Z]:)\\s*ist");
	
	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public WebDavMount mount(URI uri) throws CommandFailedException {
		final Script mountScript = fromLines(
				"net use * %URI% /persistent:no",
				"if %errorLevel% neq 0 exit %errorLevel%")
				.addEnv("URI", toHttpUri(uri));
		final CommandResult mountResult = mountScript.execute();
		mountResult.assertOk();
		final String driveLetter = getDriveLetter(mountResult.getOutput());
		final Script unmountScript = fromLines(
				"net use "+driveLetter+" /delete",
				"if %errorLevel% neq 0 exit %errorLevel%")
				.addEnv("DRIVE_LETTER", driveLetter);
		return new WebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				unmountScript.execute().assertOk();
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

	private String toHttpUri(URI uri) {
		if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
			return uri.toString();
		} else if ("dav".equals(uri.getScheme())) {
			return replaceScheme(uri, "http").toString();
		} else if ("davs".equals(uri.getScheme())) {
			return replaceScheme(uri, "https").toString();
		} else {
			throw new IllegalStateException(format("No webdav uri %s", uri));
		}
	}

	private URI replaceScheme(URI uri, String scheme) {
		try {
			return new URI(scheme,
					uri.getUserInfo(),
					uri.getHost(),
					uri.getPort(),
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Building an URI with replaced scheme failed");
		}
	}

}
