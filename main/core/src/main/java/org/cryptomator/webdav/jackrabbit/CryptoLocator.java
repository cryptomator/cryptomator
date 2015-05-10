package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.apache.logging.log4j.util.Strings;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.exceptions.IORuntimeException;

class CryptoLocator implements DavResourceLocator {

	private final CryptoLocatorFactory factory;
	private final Cryptor cryptor;
	private final Path rootPath;
	private final String prefix;
	private final String resourcePath;

	public CryptoLocator(CryptoLocatorFactory factory, Cryptor cryptor, Path rootPath, String prefix, String resourcePath) {
		this.factory = factory;
		this.cryptor = cryptor;
		this.rootPath = rootPath;
		this.prefix = prefix;
		this.resourcePath = FilenameUtils.normalizeNoEndSeparator(resourcePath, true);
	}

	/* path variants */

	/**
	 * Returns the decrypted path without any trailing slash.
	 * 
	 * @see #getHref(boolean)
	 * @return Plaintext resource path.
	 */
	@Override
	public String getResourcePath() {
		return resourcePath;
	}

	/**
	 * Returns the decrypted path and adds URL-encoding.
	 * 
	 * @param isCollection If true, a trailing slash will be appended.
	 * @see #getResourcePath()
	 * @return URL-encoded plaintext resource path.
	 */
	@Override
	public String getHref(boolean isCollection) {
		final String encodedResourcePath = EncodeUtil.escapePath(getResourcePath());
		final String href = getPrefix().concat(encodedResourcePath);
		assert !href.endsWith("/");
		if (isCollection) {
			return href.concat("/");
		} else {
			return href;
		}
	}

	/**
	 * Returns the encrypted, absolute path on the local filesystem.
	 * 
	 * @return Absolute, encrypted path as string (use {@link #getEncryptedFilePath()} for {@link Path}s).
	 */
	@Override
	public String getRepositoryPath() {
		if (isRootLocation()) {
			return getDirectoryPath();
		}
		try {
			final String plaintextPath = getResourcePath();
			final String plaintextDir = FilenameUtils.getPathNoEndSeparator(plaintextPath);
			final String plaintextFilename = FilenameUtils.getName(plaintextPath);
			final String ciphertextDir = cryptor.encryptDirectoryPath(plaintextDir, FileSystems.getDefault().getSeparator());
			final String ciphertextFilename = cryptor.encryptFilename(plaintextFilename, factory);
			final String ciphertextPath = ciphertextDir + FileSystems.getDefault().getSeparator() + ciphertextFilename;
			return rootPath.resolve(ciphertextPath).toString();
		} catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	/**
	 * Returns the encrypted, absolute path on the local filesystem to the directory represented by this locator.
	 * 
	 * @return Absolute, encrypted path as string (use {@link #getEncryptedDirectoryPath()} for {@link Path}s).
	 */
	public String getDirectoryPath() {
		final String ciphertextPath = cryptor.encryptDirectoryPath(getResourcePath(), FileSystems.getDefault().getSeparator());
		return rootPath.resolve(ciphertextPath).toString();
	}

	public Path getEncryptedFilePath() {
		return FileSystems.getDefault().getPath(getRepositoryPath());
	}

	public Path getEncryptedDirectoryPath() {
		return FileSystems.getDefault().getPath(getDirectoryPath());
	}

	/* other stuff */

	@Override
	public String getPrefix() {
		return prefix;
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
	public boolean isRootLocation() {
		return Strings.isEmpty(getResourcePath());
	}

	@Override
	public CryptoLocatorFactory getFactory() {
		return factory;
	}

	/* hashcode and equals */

	@Override
	public int hashCode() {
		final HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(prefix);
		builder.append(resourcePath);
		return builder.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CryptoLocator) {
			final CryptoLocator other = (CryptoLocator) obj;
			final EqualsBuilder builder = new EqualsBuilder();
			builder.append(this.factory, other.factory);
			builder.append(this.prefix, other.prefix);
			builder.append(this.resourcePath, other.resourcePath);
			return builder.isEquals();
		} else {
			return false;
		}
	}

}
