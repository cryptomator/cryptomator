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
		this.pathPrefix = pathPrefix;
	}

	// resourcePath == repositoryPath. No encryption here.

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		final String relativeHref = StringUtils.removeStart(href, fullPrefix);

		final String relativeCleartextPath = EncodeUtil.unescape(StringUtils.removeStart(relativeHref, "/"));
		return new CleartextLocator(relativeCleartextPath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		return new CleartextLocator(resourcePath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		return new CleartextLocator(path);
	}

	private class CleartextLocator implements DavResourceLocator {

		private final String relativeCleartextPath;

		private CleartextLocator(String relativeCleartextPath) {
			this.relativeCleartextPath = FilenameUtils.normalizeNoEndSeparator(relativeCleartextPath, true);
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
			final String encodedResourcePath = EncodeUtil.escapePath(getResourcePath());
			final String fullPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
			final String href = fullPrefix.concat(encodedResourcePath);
			assert href.equals(fullPrefix) || !href.endsWith("/");
			if (isCollection) {
				return href.concat("/");
			} else {
				return href;
			}
		}

		@Override
		public boolean isRootLocation() {
			return Strings.isEmpty(relativeCleartextPath);
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
