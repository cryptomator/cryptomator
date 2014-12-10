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

import static org.cryptomator.ui.util.CommandUtil.exec;

import java.net.URI;

import org.apache.commons.lang3.SystemUtils;

final class MacOsXWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		return SystemUtils.IS_OS_MAC_OSX;
	}

	@Override
	public WebDavMount mount(URI uri) throws CommandFailedException {
		final String path = "/Volumes/Cryptomator" + uri.getPort();
		exec("mkdir", "/Volumes/Cryptomator" + uri.getPort());
		exec("mount_webdav", "-S", "-v", "Cryptomator", uri.toString(), path);
		exec("open", path);
		return new WebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				exec("unmount", path);
			}
		};
	}

}
