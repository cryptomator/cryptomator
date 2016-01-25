/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.jackrabbit;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.cryptomator.filesystem.Folder;

public class FileSystemResourceLocatorFactory implements DavLocatorFactory {

	private final FileSystemLocator fs;

	public FileSystemResourceLocatorFactory(URI contextRootUri, Folder root) {
		String pathPrefix = StringUtils.removeEnd(contextRootUri.toString(), "/");
		this.fs = new FileSystemLocator(this, pathPrefix, root);
	}

	@Override
	public FileSystemResourceLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = StringUtils.removeEnd(prefix, "/");
		final String remainingHref = StringUtils.removeStart(href, fullPrefix);
		final String unencodedRemaingingHref = EncodeUtil.unescape(remainingHref);
		return createResourceLocator(unencodedRemaingingHref);
	}

	@Override
	public FileSystemResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		return createResourceLocator(resourcePath);
	}

	@Override
	public FileSystemResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		return createResourceLocator(path);
	}

	private FileSystemResourceLocator createResourceLocator(String path) {
		if (StringUtils.isEmpty(path) || "/".equals(path)) {
			return fs;
		} else if (path.endsWith("/")) {
			return fs.resolveFolder(path);
		} else {
			return fs.resolveFile(path);
		}

	}

}
