/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;

/**
 * A LocatorFactory constructing Locators, whose {@link DavResourceLocator#getResourcePath() resourcePath} and {@link DavResourceLocator#getRepositoryPath() repositoryPath} are equal.
 * These paths will be plain, case-sensitive, absolute, unencoded Strings with Unix-style path separators.
 * 
 * Paths ending on "/" are treated as directory paths and all others as file paths.
 */
class DavPathFactory implements DavLocatorFactory {

	private final String pathPrefix;

	public DavPathFactory(URI contextRootUri) {
		this.pathPrefix = StringUtils.removeEnd(contextRootUri.toString(), "/");
	}

	@Override
	public DavPath createResourceLocator(String prefix, String href) {
		final String fullPrefix = StringUtils.removeEnd(prefix, "/");
		final String remainingHref = StringUtils.removeStart(href, fullPrefix);
		final String unencodedRemaingingHref = EncodeUtil.unescape(remainingHref);
		return new DavPath(unencodedRemaingingHref);
	}

	@Override
	public DavPath createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		return createResourceLocator(prefix, workspacePath, resourcePath, true);
	}

	@Override
	public DavPath createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		return new DavPath(path);
	}

	public class DavPath implements DavResourceLocator {

		private final String absPath;

		private DavPath(String absPath) {
			assert absPath.startsWith("/");
			this.absPath = FilenameUtils.normalize(absPath, true);
		}

		/**
		 * @return <code>true</code> if the path ends on "/".
		 */
		public boolean isDirectory() {
			return absPath.endsWith("/");
		}

		/**
		 * @return Parent DavPath or <code>null</code> if this is the root node.
		 */
		public DavPath getParent() {
			if (isRootLocation()) {
				return null;
			} else {
				final String parentPath = FilenameUtils.getFullPath(FilenameUtils.normalizeNoEndSeparator(absPath, true));
				return createResourceLocator(getPrefix(), getWorkspacePath(), parentPath);
			}
		}

		/**
		 * Get the path of a child resource, consisting of curren path + child path.
		 * If the child path ends on "/", the returned DavPath will be a directory path.
		 * 
		 * @return Child path
		 */
		public DavPath getChild(String relativeChildPath) {
			if (isDirectory()) {
				final String absChildPath = absPath + StringUtils.removeStart(relativeChildPath, "/");
				return createResourceLocator(getPrefix(), getWorkspacePath(), absChildPath);
			} else {
				throw new UnsupportedOperationException("Can only resolve subpaths of a path representing a directory");
			}
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

		/**
		 * @see #getHref(boolean)
		 */
		public String getHref() {
			return getHref(isDirectory());
		}

		@Override
		public String getHref(boolean isCollection) {
			final String encodedResourcePath = EncodeUtil.escapePath(absPath);
			if (isRootLocation()) {
				return pathPrefix + "/";
			} else {
				assert isCollection ? isDirectory() : true;
				return pathPrefix + encodedResourcePath;
			}
		}

		@Override
		public boolean isRootLocation() {
			return "/".equals(absPath);
		}

		@Override
		public DavPathFactory getFactory() {
			return DavPathFactory.this;
		}

		@Override
		public String getRepositoryPath() {
			return absPath;
		}

		@Override
		public String toString() {
			return "[" + pathPrefix + "]" + absPath;
		}

		@Override
		public int hashCode() {
			return absPath.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DavPath) {
				final DavPath other = (DavPath) obj;
				final boolean samePrefix = this.getPrefix() == null && other.getPrefix() == null || this.getPrefix().equals(other.getPrefix());
				final boolean sameRelativeCleartextPath = this.absPath == null && other.absPath == null || this.absPath.equals(other.absPath);
				return samePrefix && sameRelativeCleartextPath;
			} else {
				return false;
			}
		}
	}

}
