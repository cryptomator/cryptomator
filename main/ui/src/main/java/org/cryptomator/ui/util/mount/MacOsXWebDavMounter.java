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

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.command.Script;

final class MacOsXWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_MAC_OSX;
	}

	@Override
	public WebDavMount mount(int localPort) throws CommandFailedException {
		final String path = "/Volumes/Cryptomator" + localPort;
		final Script mountScript = Script.fromLines(
				"set -x",
				"mkdir \"$MOUNT_PATH\"",
				"mount_webdav -S -v Cryptomator \"[::1]:$PORT\" \"$MOUNT_PATH\"",
				"open \"$MOUNT_PATH\"")
				.addEnv("PORT", String.valueOf(localPort))
				.addEnv("MOUNT_PATH", path);
		final Script unmountScript = Script.fromLines(
				"set -x",
				"umount $MOUNT_PATH")
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
