package org.cryptomator.webdav.jackrabbit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.apache.logging.log4j.util.Strings;

public class CleartextLocatorFactory implements DavLocatorFactory {

	private final String pathPrefix;

	public CleartextLocatorFactory(String pathPrefix) {
		this.pathPrefix = StringUtils.removeEnd(pathPrefix, "/");
	}

	// resourcePath == repositoryPath. No encryption here.

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = StringUtils.removeEnd(prefix, "/");
		final String relativeHref = StringUtils.removeStart(href, fullPrefix);

		final String relativeCleartextPath = EncodeUtil.unescape(relativeHref);
		assert relativeCleartextPath.startsWith("/");
		return new CleartextLocator(relativeCleartextPath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		assert resourcePath.startsWith("/");
		return new CleartextLocator(resourcePath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		assert path.startsWith("/");
		return new CleartextLocator(path);
	}

	private class CleartextLocator implements DavResourceLocator {

		private final String relativeCleartextPath;

		private CleartextLocator(String relativeCleartextPath) {
			this.relativeCleartextPath = StringUtils.prependIfMissing(FilenameUtils.normalizeNoEndSeparator(relativeCleartextPath, true), "/");
		}

		@Override
		public String getPrefix() {
			return pathPrefix;
		}

		@Override
		public String getResourcePath() {
			return relativeCleartextPath;
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
			final String encodedResourcePath = EncodeUtil.escapePath(relativeCleartextPath);
			if (isRootLocation()) {
				return pathPrefix + "/";
			} else if (isCollection) {
				return pathPrefix + encodedResourcePath + "/";
			} else {
				return pathPrefix + encodedResourcePath;
			}
		}

		@Override
		public boolean isRootLocation() {
			return "/".equals(relativeCleartextPath);
		}

		@Override
		public DavLocatorFactory getFactory() {
			return CleartextLocatorFactory.this;
		}

		@Override
		public String getRepositoryPath() {
			return relativeCleartextPath;
		}

		@Override
		public String toString() {
			return "Locator: " + relativeCleartextPath + " (Prefix: " + pathPrefix + ")";
		}

		@Override
		public int hashCode() {
			return relativeCleartextPath.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CleartextLocator) {
				final CleartextLocator other = (CleartextLocator) obj;
				return relativeCleartextPath == null && other.relativeCleartextPath == null || relativeCleartextPath.equals(other.relativeCleartextPath);
			} else {
				return false;
			}
		}
	}

}
