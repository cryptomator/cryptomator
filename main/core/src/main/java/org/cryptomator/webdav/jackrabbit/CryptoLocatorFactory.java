package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.CryptorMetadataSupport;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.webdav.exceptions.DecryptFailedRuntimeException;
import org.cryptomator.webdav.exceptions.IORuntimeException;

class CryptoLocatorFactory implements DavLocatorFactory, CryptorMetadataSupport {

	private final Path dataRoot;
	private final Path metadataRoot;
	private final Cryptor cryptor;

	CryptoLocatorFactory(String fsRoot, Cryptor cryptor) {
		this.dataRoot = FileSystems.getDefault().getPath(fsRoot).resolve("d");
		this.metadataRoot = FileSystems.getDefault().getPath(fsRoot).resolve("m");
		this.cryptor = cryptor;
	}

	@Override
	public CryptoLocator createResourceLocator(String prefix, String href) {
		final String fullPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		final String relativeHref = StringUtils.removeStart(href, fullPrefix);

		final String resourcePath = EncodeUtil.unescape(StringUtils.removeStart(relativeHref, "/"));
		return new CryptoLocator(this, cryptor, dataRoot, fullPrefix, resourcePath);
	}

	/**
	 * @throws DecryptFailedRuntimeException, which should be a checked exception, but Jackrabbit doesn't allow that.
	 */
	@Override
	public CryptoLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		if (!isResourcePath) {
			throw new UnsupportedOperationException("Can not decrypt " + path + " without knowing plaintext parent path.");
		}
		final String fullPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		return new CryptoLocator(this, cryptor, dataRoot, fullPrefix, path);
	}

	@Override
	public CryptoLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		try {
			return createResourceLocator(prefix, workspacePath, resourcePath, true);
		} catch (DecryptFailedRuntimeException e) {
			throw new IllegalStateException("Tried to decrypt resourcePath. Only repositoryPaths can be encrypted.", e);
		}
	}

	public DavResourceLocator createSubresourceLocator(CryptoLocator parentResource, String ciphertextChildName) {
		try {
			final String plaintextFilename = cryptor.decryptFilename(ciphertextChildName, this);
			final String plaintextPath = FilenameUtils.concat(parentResource.getResourcePath(), plaintextFilename);
			return createResourceLocator(parentResource.getPrefix(), parentResource.getWorkspacePath(), plaintextPath);
		} catch (IOException e) {
			throw new IORuntimeException(e);
		} catch (DecryptFailedException e) {
			throw new DecryptFailedRuntimeException(e);
		}
	}

	/* metadata storage */

	@Override
	public void writeMetadata(String metadataGroup, byte[] encryptedMetadata) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		Files.createDirectories(metadataDir);
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		try (final FileChannel c = FileChannel.open(metadataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
			c.write(ByteBuffer.wrap(encryptedMetadata));
		}
	}

	@Override
	public byte[] readMetadata(String metadataGroup) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		if (!Files.isReadable(metadataFile)) {
			return null;
		}
		try (final FileChannel c = FileChannel.open(metadataFile, StandardOpenOption.READ, StandardOpenOption.DSYNC); final FileLock lock = c.lock(0L, Long.MAX_VALUE, true)) {
			final ByteBuffer buffer = ByteBuffer.allocate((int) c.size());
			c.read(buffer);
			return buffer.array();
		}
	}
}
