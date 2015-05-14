package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FilenameUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.logging.log4j.util.Strings;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.CryptorMetadataSupport;
import org.eclipse.jetty.http.HttpHeader;

public class CryptoResourceFactory implements DavResourceFactory, CryptorMetadataSupport {

	private final LockManager lockManager = new SimpleLockManager();
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;
	private final ExecutorService backgroundTaskExecutor;
	private final Path dataRoot;
	private final Path metadataRoot;

	CryptoResourceFactory(Cryptor cryptor, CryptoWarningHandler cryptoWarningHandler, ExecutorService backgroundTaskExecutor, String fsRoot) {
		this.cryptor = cryptor;
		this.cryptoWarningHandler = cryptoWarningHandler;
		this.backgroundTaskExecutor = backgroundTaskExecutor;
		this.dataRoot = FileSystems.getDefault().getPath(fsRoot).resolve("d");
		this.metadataRoot = FileSystems.getDefault().getPath(fsRoot).resolve("m");
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		if (DavMethods.METHOD_MKCOL.equals(request.getMethod()) || locator.isRootLocation()) {
			final Path dirpath = getEncryptedDirectoryPath(locator.getResourcePath());
			return createDirectory(locator, request.getDavSession(), dirpath);
		}

		final Path filepath = getEncryptedFilePath(locator.getResourcePath());
		final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());
		if (filepath.getFileName().toString().endsWith(".dir")) {
			final Path dirpath = getEncryptedDirectoryPath(locator.getResourcePath());
			return createDirectory(locator, request.getDavSession(), dirpath);
		} else if (Files.isRegularFile(filepath) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null) {
			response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
			return createFilePart(locator, request.getDavSession(), request, filepath);
		} else if (Files.isRegularFile(filepath) || DavMethods.METHOD_PUT.equals(request.getMethod())) {
			return createFile(locator, request.getDavSession(), filepath);
		} else {
			return createNonExisting(locator, request.getDavSession());
		}
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (locator.isRootLocation()) {
			final Path dirpath = getEncryptedDirectoryPath(locator.getResourcePath());
			return createDirectory(locator, session, dirpath);
		}

		final Path filepath = getEncryptedFilePath(locator.getResourcePath());
		if (filepath.getFileName().toString().endsWith(".dir")) {
			final Path dirpath = getEncryptedDirectoryPath(locator.getResourcePath());
			return createDirectory(locator, session, dirpath);
		} else if (Files.isRegularFile(filepath)) {
			return createFile(locator, session, filepath);
		} else {
			return createNonExisting(locator, session);
		}
	}

	/**
	 * @return Absolute file path for a given cleartext file resourcePath.
	 * @throws IOException
	 */
	Path getEncryptedFilePath(String relativeCleartextPath) throws DavException {
		final String parentCleartextPath = FilenameUtils.getPathNoEndSeparator(relativeCleartextPath);
		final Path parent = getEncryptedDirectoryPath(parentCleartextPath);
		final String cleartextFilename = FilenameUtils.getName(relativeCleartextPath);
		try {
			final String encryptedFilename = cryptor.encryptFilename(cleartextFilename, this);
			return parent.resolve(encryptedFilename);
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * @return Absolute directory path for a given cleartext directory resourcePath.
	 * @throws IOException
	 */
	Path getEncryptedDirectoryPath(String relativeCleartextPath) throws DavException {
		assert Strings.isEmpty(relativeCleartextPath) || !relativeCleartextPath.endsWith("/");
		try {
			final Path result;
			if (Strings.isEmpty(relativeCleartextPath)) {
				// root level
				final String fixedRootDirectory = cryptor.encryptDirectoryPath("", FileSystems.getDefault().getSeparator());
				result = dataRoot.resolve(fixedRootDirectory);
			} else {
				final String parentCleartextPath = FilenameUtils.getPathNoEndSeparator(relativeCleartextPath);
				final Path parent = getEncryptedDirectoryPath(parentCleartextPath);
				final String cleartextFilename = FilenameUtils.getName(relativeCleartextPath);
				final String encryptedFilename = cryptor.encryptFilename(cleartextFilename, CryptoResourceFactory.this);
				final Path directoryFile = parent.resolve(encryptedFilename);
				final String directoryId;
				if (Files.exists(directoryFile)) {
					directoryId = new String(readAllBytesAtomically(directoryFile), StandardCharsets.UTF_8);
				} else {
					directoryId = UUID.randomUUID().toString();
					writeAllBytesAtomically(directoryFile, directoryId.getBytes(StandardCharsets.UTF_8));
				}
				final String directory = cryptor.encryptDirectoryPath(directoryId, FileSystems.getDefault().getSeparator());
				result = dataRoot.resolve(directory);
			}
			Files.createDirectories(result);
			return result;
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	private EncryptedFile createFilePart(DavResourceLocator locator, DavSession session, DavServletRequest request, Path filePath) {
		return new EncryptedFilePart(this, locator, session, request, lockManager, cryptor, cryptoWarningHandler, backgroundTaskExecutor, filePath);
	}

	private EncryptedFile createFile(DavResourceLocator locator, DavSession session, Path filePath) {
		return new EncryptedFile(this, locator, session, lockManager, cryptor, cryptoWarningHandler, filePath);
	}

	private EncryptedDir createDirectory(DavResourceLocator locator, DavSession session, Path dirPath) {
		return new EncryptedDir(this, locator, session, lockManager, cryptor, dirPath);
	}

	private NonExistingNode createNonExisting(DavResourceLocator locator, DavSession session) {
		return new NonExistingNode(this, locator, session, lockManager, cryptor);
	}

	/* IO support */

	private void writeAllBytesAtomically(Path path, byte[] bytes) throws IOException {
		try (final FileChannel c = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
			c.write(ByteBuffer.wrap(bytes));
		}
	}

	private byte[] readAllBytesAtomically(Path path) throws IOException {
		try (final FileChannel c = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.DSYNC); final FileLock lock = c.lock(0L, Long.MAX_VALUE, true)) {
			final ByteBuffer buffer = ByteBuffer.allocate((int) c.size());
			c.read(buffer);
			return buffer.array();
		}
	}

	@Override
	public void writeMetadata(String metadataGroup, byte[] encryptedMetadata) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		Files.createDirectories(metadataDir);
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		writeAllBytesAtomically(metadataFile, encryptedMetadata);
	}

	@Override
	public byte[] readMetadata(String metadataGroup) throws IOException {
		final Path metadataDir = metadataRoot.resolve(metadataGroup.substring(0, 2));
		final Path metadataFile = metadataDir.resolve(metadataGroup.substring(2));
		if (!Files.isReadable(metadataFile)) {
			return null;
		} else {
			return readAllBytesAtomically(metadataFile);
		}
	}

}
