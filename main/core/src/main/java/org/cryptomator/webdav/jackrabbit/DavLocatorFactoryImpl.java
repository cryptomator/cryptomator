/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.CryptorIOSupport;
import org.cryptomator.crypto.SensitiveDataSwipeListener;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.webdav.exceptions.DecryptFailedRuntimeException;

class DavLocatorFactoryImpl implements DavLocatorFactory, SensitiveDataSwipeListener, CryptorIOSupport {

	private static final int MAX_CACHED_PATHS = 10000;
	private final Path fsRoot;
	private final Cryptor cryptor;
	private final BidiMap<String, String> pathCache = new BidiLRUMap<>(MAX_CACHED_PATHS); // <decryptedPath, encryptedPath>

	DavLocatorFactoryImpl(String fsRoot, Cryptor cryptor) {
		this.fsRoot = FileSystems.getDefault().getPath(fsRoot);
		this.cryptor = cryptor;
		cryptor.addSensitiveDataSwipeListener(this);
	}

	/* DavLocatorFactory */

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		final String relativeHref = StringUtils.removeStart(href, fullPrefix);

		final String resourcePath = EncodeUtil.unescape(StringUtils.removeStart(relativeHref, "/"));
		return new DavResourceLocatorImpl(fullPrefix, resourcePath);
	}

	/**
	 * @throws DecryptFailedRuntimeException, which should a checked exception, but Jackrabbit doesn't allow that.
	 */
	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		final String fullPrefix = prefix.endsWith("/") ? prefix : prefix + "/";

		try {
			final String resourcePath = (isResourcePath) ? path : getResourcePath(path);
			return new DavResourceLocatorImpl(fullPrefix, resourcePath);
		} catch (DecryptFailedException e) {
			throw new DecryptFailedRuntimeException(e);
		}
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		try {
			return createResourceLocator(prefix, workspacePath, resourcePath, true);
		} catch (DecryptFailedRuntimeException e) {
			throw new IllegalStateException("Tried to decrypt resourcePath. Only repositoryPaths can be encrypted.", e);
		}
	}

	/* Encryption/Decryption */

	/**
	 * @return Encrypted absolute paths on the file system.
	 */
	private String getRepositoryPath(String resourcePath) {
		String encryptedPath = pathCache.get(resourcePath);
		if (encryptedPath == null) {
			encryptedPath = encryptRepositoryPath(resourcePath);
			pathCache.put(resourcePath, encryptedPath);
		}
		return encryptedPath;
	}

	private String encryptRepositoryPath(String resourcePath) {
		if (resourcePath == null) {
			return fsRoot.toString();
		}
		final String encryptedRepoPath = cryptor.encryptPath(resourcePath, FileSystems.getDefault().getSeparator().charAt(0), '/', this);
		return fsRoot.resolve(encryptedRepoPath).toString();
	}

	/**
	 * @return Decrypted path for use in URIs.
	 */
	private String getResourcePath(String repositoryPath) throws DecryptFailedException {
		String decryptedPath = pathCache.getKey(repositoryPath);
		if (decryptedPath == null) {
			decryptedPath = decryptResourcePath(repositoryPath);
			pathCache.put(decryptedPath, repositoryPath);
		}
		return decryptedPath;
	}

	private String decryptResourcePath(String repositoryPath) throws DecryptFailedException {
		final Path absRepoPath = FileSystems.getDefault().getPath(repositoryPath);
		if (fsRoot.equals(absRepoPath)) {
			return null;
		} else {
			final Path relativeRepositoryPath = fsRoot.relativize(absRepoPath);
			final String resourcePath = cryptor.decryptPath(relativeRepositoryPath.toString(), FileSystems.getDefault().getSeparator().charAt(0), '/', this);
			return resourcePath;
		}
	}

	/* CryptorIOSupport */

	@Override
	public void writePathSpecificMetadata(String encryptedPath, byte[] encryptedMetadata) throws IOException {
		final Path metaDataFile = fsRoot.resolve(encryptedPath);
		Files.write(metaDataFile, encryptedMetadata, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC);
	}

	@Override
	public byte[] readPathSpecificMetadata(String encryptedPath) throws IOException {
		final Path metaDataFile = fsRoot.resolve(encryptedPath);
		if (!Files.isReadable(metaDataFile)) {
			return null;
		} else {
			return Files.readAllBytes(metaDataFile);
		}
	}

	/* SensitiveDataSwipeListener */

	@Override
	public void swipeSensitiveData() {
		pathCache.clear();
	}

	/* Locator */

	private class DavResourceLocatorImpl implements DavResourceLocator {

		private final String prefix;
		private final String resourcePath;

		private DavResourceLocatorImpl(String prefix, String resourcePath) {
			this.prefix = prefix;
			this.resourcePath = resourcePath;
		}

		@Override
		public String getPrefix() {
			return prefix;
		}

		@Override
		public String getResourcePath() {
			return resourcePath;
		}

		@Override
		public String getWorkspacePath() {
			return isRootLocation() ? null : "";
		}

		@Override
		public String getWorkspaceName() {
			return getPrefix();
		}

		@Override
		public boolean isSameWorkspace(DavResourceLocator locator) {
			return (locator == null) ? false : isSameWorkspace(locator.getWorkspaceName());
		}

		@Override
		public boolean isSameWorkspace(String workspaceName) {
			return getWorkspaceName().equals(workspaceName);
		}

		@Override
		public String getHref(boolean isCollection) {
			final String href = getPrefix().concat(getResourcePath());
			if (isCollection && !href.endsWith("/")) {
				return href.concat("/");
			} else if (!isCollection && href.endsWith("/")) {
				return href.substring(0, href.length() - 1);
			} else {
				return href;
			}
		}

		@Override
		public boolean isRootLocation() {
			return getResourcePath() == null;
		}

		@Override
		public DavLocatorFactory getFactory() {
			return DavLocatorFactoryImpl.this;
		}

		@Override
		public String getRepositoryPath() {
			return DavLocatorFactoryImpl.this.getRepositoryPath(getResourcePath());
		}

		@Override
		public int hashCode() {
			final HashCodeBuilder builder = new HashCodeBuilder();
			builder.append(prefix);
			builder.append(resourcePath);
			return builder.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DavResourceLocatorImpl) {
				final DavResourceLocatorImpl other = (DavResourceLocatorImpl) obj;
				final EqualsBuilder builder = new EqualsBuilder();
				builder.append(this.prefix, other.prefix);
				builder.append(this.resourcePath, other.resourcePath);
				return builder.isEquals();
			} else {
				return false;
			}
		}

	}

}
