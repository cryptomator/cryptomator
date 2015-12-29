/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class DelegatingFileSystem extends DelegatingFolder implements FileSystem {

	public DelegatingFileSystem(Folder delegate) {
		super(null, delegate);
	}

}
