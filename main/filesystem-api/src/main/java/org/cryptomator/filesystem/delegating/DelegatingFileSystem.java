/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.delegating;

import java.util.function.BiFunction;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class DelegatingFileSystem extends DelegatingFolder implements FileSystem {

	private DelegatingFileSystem(Folder delegate, BiFunction<DelegatingFolder, Folder, DelegatingFolder> folderFactory, BiFunction<DelegatingFolder, File, DelegatingFile> fileFactory) {
		super(null, delegate, folderFactory, fileFactory);
	}

	public static DelegatingFileSystem withDelegate(Folder delegate) {
		return new DelegatingFileSystem(delegate, DelegatingFileSystem::subFolder, DelegatingFileSystem::subFile);
	}

	private static DelegatingFolder subFolder(DelegatingFolder parent, Folder delegateSubFolder) {
		return new DelegatingFolder(parent, delegateSubFolder, DelegatingFileSystem::subFolder, DelegatingFileSystem::subFile);
	}

	private static DelegatingFile subFile(DelegatingFolder parent, File delegateSubFile) {
		return new DelegatingFile(parent, delegateSubFile, DelegatingReadableFile::new, DelegatingWritableFile::new);
	}

}
