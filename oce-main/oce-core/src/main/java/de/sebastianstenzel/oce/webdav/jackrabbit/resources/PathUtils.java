/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav.jackrabbit.resources;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;

public final class PathUtils {

	private PathUtils() {
		throw new IllegalStateException("not instantiable");
	}

	public static Path getPhysicalPath(DavResource resource) {
		return getPhysicalPath(resource.getLocator());
	}

	public static Path getPhysicalPath(DavResourceLocator locator) {
		return FileSystems.getDefault().getPath(locator.getRepositoryPath());
	}

}
