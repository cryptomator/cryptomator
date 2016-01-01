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
		}
		final FolderLocator folder = fs.resolveFolder(path);
		final FileLocator file = fs.resolveFile(path);
		if (folder.exists()) {
			return folder;
		} else if (file.exists()) {
			return file;
		} else if (path.endsWith("/")) {
			return folder;
		} else {
			return file;
		}

	}

}
