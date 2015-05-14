package org.cryptomator.webdav.jackrabbit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
			return getEncryptedRootDirectoryPath();
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
	 * @throws IOException
	 */
	public String getDirectoryPath(boolean create) throws IOException {
		if (isRootLocation()) {
			return getEncryptedRootDirectoryPath();
		} else {
			final List<String> cleartextPathComponents = Arrays.asList(StringUtils.split(getResourcePath(), "/"));
			return getEncryptedDirectoryPath(rootPath, cleartextPathComponents, false).toString();
		}
	}

	private Path getEncryptedDirectoryPath(Path encryptedParentDirectoryPath, List<String> cleartextSubPathComponents, boolean create) throws IOException {
		if (cleartextSubPathComponents.size() == 0) {
			return encryptedParentDirectoryPath;
		} else {
			final String nextPathComponent = cleartextSubPathComponents.get(0);
			final List<String> remainingSubPathComponents = cleartextSubPathComponents.subList(1, cleartextSubPathComponents.size());
			final String fullEncryptedSubdirectoryPath = getEncryptedDirectoryPath(encryptedParentDirectoryPath, nextPathComponent, create);
			return getEncryptedDirectoryPath(rootPath.resolve(fullEncryptedSubdirectoryPath), remainingSubPathComponents, create);
		}
	}

	private String getEncryptedDirectoryPath(Path encryptedParentDirectoryPath, String cleartextDirectoryName, boolean create) throws IOException {
		final String encryptedDirectoryName = this.cryptor.encryptFilename(cleartextDirectoryName, this.factory);
		// TODO file extensions...
		final Path directoryFile = encryptedParentDirectoryPath.resolve(encryptedDirectoryName + ".dir");
		if (Files.exists(directoryFile)) {
			try (final FileChannel c = FileChannel.open(directoryFile, StandardOpenOption.READ, StandardOpenOption.DSYNC); final FileLock lock = c.lock(0L, Long.MAX_VALUE, true)) {
				final ByteBuffer buffer = ByteBuffer.allocate((int) c.size());
				c.read(buffer);
				final String directoryUuid = buffer.asCharBuffer().toString();
				return this.cryptor.encryptDirectoryPath(directoryUuid, FileSystems.getDefault().getSeparator());
			}
		} else if (create) {
			try (final FileChannel c = FileChannel.open(directoryFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
				final String directoryUuid = UUID.randomUUID().toString();
				final ByteBuffer buf = ByteBuffer.wrap(directoryUuid.getBytes(StandardCharsets.UTF_8));
				c.write(buf);
				return this.cryptor.encryptDirectoryPath(directoryUuid, FileSystems.getDefault().getSeparator());
			}
		} else {
			throw new FileNotFoundException(directoryFile.toString());
		}
	}

	private String getEncryptedRootDirectoryPath() {
		return this.cryptor.encryptDirectoryPath("", FileSystems.getDefault().getSeparator());
	}

	public Path getEncryptedFilePath() {
		return FileSystems.getDefault().getPath(getRepositoryPath());
	}

	public Path getEncryptedDirectoryPath(boolean create) throws IOException {
		return FileSystems.getDefault().getPath(getDirectoryPath(create));
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
