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

final class LinuxGvfsWebDavMounter implements WebDavMounterStrategy {

	@Override
	public boolean shouldWork() {
		// TODO check result of "which gvfs-mount"
		return SystemUtils.IS_OS_LINUX;
	}

	@Override
	public WebDavMount mount(final URI uri) throws CommandFailedException {
		exec("gvfs-mount", uri.toString());
		exec("xdg-open", uri.toString());
		return new WebDavMount() {
			@Override
			public void unmount() throws CommandFailedException {
				exec("gvfs-mount", "-u", uri.toString());
			}
		};
	}

}
