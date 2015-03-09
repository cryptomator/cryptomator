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

import java.net.URI;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.util.command.Script;

final class MacOsXWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_MAC_OSX;
	}

	@Override
	public void warmUp(int serverPort) {
		// no-op
	}

	@Override
	public WebDavMount mount(URI uri, String name) throws CommandFailedException {
		final String path = "/Volumes/Cryptomator" + uri.getRawPath().replace('/', '_');
		final Script mountScript = Script.fromLines(
				"mkdir \"$MOUNT_PATH\"",
				"mount_webdav -S -v $MOUNT_NAME \"$DAV_AUTHORITY$DAV_PATH\" \"$MOUNT_PATH\"",
				"open \"$MOUNT_PATH\"")
				.addEnv("DAV_AUTHORITY", uri.getRawAuthority())
				.addEnv("DAV_PATH", uri.getRawPath())
				.addEnv("MOUNT_PATH", path)
				.addEnv("MOUNT_NAME", name);
		final Script unmountScript = Script.fromLines(
				"umount $MOUNT_PATH")
				.addEnv("MOUNT_PATH", path);
		mountScript.execute();
		return new AbstractWebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				unmountScript.execute();
			}
		};
	}

}
