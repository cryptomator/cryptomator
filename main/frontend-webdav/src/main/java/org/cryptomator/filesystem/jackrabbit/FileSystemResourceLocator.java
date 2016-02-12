/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.jackrabbit;

import java.util.Optional;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.cryptomator.filesystem.Node;

public interface FileSystemResourceLocator extends DavResourceLocator, Node {

	@Override
	Optional<FolderLocator> parent();

	@Override
	default String getWorkspacePath() {
		return null;
	}

	@Override
	default String getWorkspaceName() {
		return null;
	}

	@Override
	default boolean isSameWorkspace(DavResourceLocator locator) {
		return false;
	}

	@Override
	default boolean isSameWorkspace(String workspaceName) {
		return false;
	}

	default String getHref() {
		final boolean isCollection = getResourcePath().endsWith("/");
		return getHref(isCollection);
	}

	@Override
	default String getHref(boolean isCollection) {
		final String encodedResourcePath = EncodeUtil.escapePath(getResourcePath());
		return getPrefix() + encodedResourcePath;
	}

	@Override
	default String getRepositoryPath() {
		return getResourcePath();
	}

}
