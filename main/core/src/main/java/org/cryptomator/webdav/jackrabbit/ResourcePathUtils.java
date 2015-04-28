/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.nio.file.FileSystems;
import java.nio.file.Path;

final class ResourcePathUtils {

	private ResourcePathUtils() {
		throw new IllegalStateException("not instantiable");
	}

	public static Path getPhysicalFilePath(CryptoLocator locator) {
		return FileSystems.getDefault().getPath(locator.getRepositoryPath());
	}

	public static Path getPhysicalDirectoryPath(CryptoLocator locator) {
		return FileSystems.getDefault().getPath(locator.getDirectoryPath());
	}

}
