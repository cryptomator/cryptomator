/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;

/**
 * A LocatorFactory constructing Locators, whose {@link DavResourceLocator#getResourcePath() resourcePath} and {@link DavResourceLocator#getRepositoryPath() repositoryPath} are equal.
 * These paths will be plain, case-sensitive, absolute, unencoded Strings with Unix-style path separators.
 */
class IdentityLocatorFactory implements DavLocatorFactory {

	private final String pathPrefix;

	public IdentityLocatorFactory(URI contextRootUri) {
		this.pathPrefix = StringUtils.removeEnd(contextRootUri.toString(), "/");
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = StringUtils.removeEnd(prefix, "/");
		final String remainingHref = StringUtils.removeStart(href, fullPrefix);
		final String unencodedRemaingingHref = EncodeUtil.unescape(remainingHref);
		assert unencodedRemaingingHref.startsWith("/");
		return new IdentityLocator(unencodedRemaingingHref);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		assert resourcePath.startsWith("/");
		return new IdentityLocator(resourcePath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		assert path.startsWith("/");
		return new IdentityLocator(path);
	}

	private class IdentityLocator implements DavResourceLocator {

		private final String absPath;

		private IdentityLocator(String absPath) {
			assert absPath.startsWith("/");
			this.absPath = FilenameUtils.normalize(absPath, true);
		}

		@Override
		public String getPrefix() {
			return pathPrefix;
		}

		@Override
		public String getResourcePath() {
			return absPath;
		}

		@Override
		public String getWorkspacePath() {
			return null;
		}

		@Override
		public String getWorkspaceName() {
			return null;
		}

		@Override
		public boolean isSameWorkspace(DavResourceLocator locator) {
			return false;
		}

		@Override
		public boolean isSameWorkspace(String workspaceName) {
			return false;
		}

		@Override
		public String getHref(boolean isCollection) {
			final String encodedResourcePath = EncodeUtil.escapePath(absPath);
			if (isRootLocation()) {
				return pathPrefix + "/";
			} else {
				assert isCollection ? encodedResourcePath.endsWith("/") : true;
				return pathPrefix + encodedResourcePath;
			}
		}

		@Override
		public boolean isRootLocation() {
			return "/".equals(absPath);
		}

		@Override
		public DavLocatorFactory getFactory() {
			return IdentityLocatorFactory.this;
		}

		@Override
		public String getRepositoryPath() {
			return absPath;
		}

		@Override
		public String toString() {
			return "Locator: " + absPath + " (Prefix: " + pathPrefix + ")";
		}

		@Override
		public int hashCode() {
			return absPath.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IdentityLocator) {
				final IdentityLocator other = (IdentityLocator) obj;
				final boolean samePrefix = this.getPrefix() == null && other.getPrefix() == null || this.getPrefix().equals(other.getPrefix());
				final boolean sameRelativeCleartextPath = this.absPath == null && other.absPath == null || this.absPath.equals(other.absPath);
				return samePrefix && sameRelativeCleartextPath;
			} else {
				return false;
			}
		}
	}

}
