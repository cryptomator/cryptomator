/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend;

import java.util.Map;
import java.util.Optional;

public interface Frontend extends AutoCloseable {

	public enum MountParam {
		MOUNT_NAME, HOSTNAME, WIN_DRIVE_LETTER
	}

	void mount(Map<MountParam, Optional<String>> map) throws CommandFailedException;

	void unmount() throws CommandFailedException;

	void reveal() throws CommandFailedException;

	// For now let's assume every single frontend knows what a WebDAV url is ;-)
	String getWebDavUrl();

}
