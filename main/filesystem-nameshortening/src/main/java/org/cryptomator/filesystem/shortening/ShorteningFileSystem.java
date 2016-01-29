/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class ShorteningFileSystem extends ShorteningFolder implements FileSystem {

	public ShorteningFileSystem(Folder root, Folder metadataRoot, int threshold) {
		super(null, root, "", new FilenameShortener(metadataRoot, threshold));
		create();
	}

}
