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

import java.net.URI;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.command.Script;

final class MacOsXWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_MAC_OSX;
	}

	@Override
	public WebDavMount mount(URI uri) throws CommandFailedException {
		final String path = "/Volumes/Cryptomator" + uri.getPort();
		final Script mountScript = Script.fromLines(
				"set -x",
				"mkdir \"$MOUNT_PATH\"",
				"mount_webdav -S -v Cryptomator \"$URI\" \"$MOUNT_PATH\"",
				"open \"$MOUNT_PATH\"")
				.addEnv("URI", uri.toString())
				.addEnv("MOUNT_PATH", path);
		final Script unmountScript = Script.fromLines(
				"set -x",
				"unmount $MOUNT_PATH")
				.addEnv("MOUNT_PATH", path);
		mountScript.execute().assertOk();
		return new WebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				unmountScript.execute().assertOk();
			}
		};
	}

}
