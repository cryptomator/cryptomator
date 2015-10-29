package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.format.DateTimeParseException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import org.cryptomator.crypto.Cryptor;
import org.eclipse.jetty.http.HttpHeader;

public class CryptoResourceFactory implements DavResourceFactory, FileConstants {

	private static final String RANGE_BYTE_PREFIX = "bytes=";
	private static final char RANGE_SET_SEP = ',';
	private static final char RANGE_SEP = '-';

	private final LockManager lockManager = new SimpleLockManager();
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;
	private final Path dataRoot;
	private final FilenameTranslator filenameTranslator;

	CryptoResourceFactory(Cryptor cryptor, CryptoWarningHandler cryptoWarningHandler, String vaultRoot) {
		Path vaultRootPath = FileSystems.getDefault().getPath(vaultRoot);
		this.cryptor = cryptor;
		this.cryptoWarningHandler = cryptoWarningHandler;
		this.dataRoot = vaultRootPath.resolve("d");
		this.filenameTranslator = new FilenameTranslator(cryptor, vaultRootPath);
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		if (locator.isRootLocation()) {
			return createRootDirectory(locator, request.getDavSession());
		}

		try {
			final Path filePath = getEncryptedFilePath(locator.getResourcePath(), false);
			final Path dirFilePath = getEncryptedDirectoryFilePath(locator.getResourcePath(), false);
			final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());
			final String ifRangeHeader = request.getHeader(HttpHeader.IF_RANGE.asString());
			if (Files.exists(dirFilePath) || DavMethods.METHOD_MKCOL.equals(request.getMethod())) {
				// DIRECTORY
				return createDirectory(locator, request.getDavSession(), dirFilePath);
			} else if (Files.exists(filePath) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null && isRangeSatisfiable(rangeHeader) && isIfRangePreconditionFulfilled(ifRangeHeader, filePath)) {
				// FILE RANGE
				final Pair<String, String> requestRange = getRequestRange(rangeHeader);
				response.setStatus(DavServletResponse.SC_PARTIAL_CONTENT);
				return createFilePart(locator, request.getDavSession(), requestRange, filePath);
			} else if (Files.exists(filePath) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null && isRangeSatisfiable(rangeHeader) && !isIfRangePreconditionFulfilled(ifRangeHeader, filePath)) {
				// FULL FILE (if-range not fulfilled)
				return createFile(locator, request.getDavSession(), filePath);
			} else if (Files.exists(filePath) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null && !isRangeSatisfiable(rangeHeader)) {
				// FULL FILE (unsatisfiable range)
				response.setStatus(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				final EncryptedFile file = createFile(locator, request.getDavSession(), filePath);
				response.addHeader(HttpHeader.CONTENT_RANGE.asString(), "bytes */" + file.getContentLength());
				return file;
			} else if (Files.exists(filePath) || DavMethods.METHOD_PUT.equals(request.getMethod())) {
				// FULL FILE (as requested)
				return createFile(locator, request.getDavSession(), filePath);
			}
		} catch (NonExistingParentException e) {
			// return non-existing
		}
		return createNonExisting(locator, request.getDavSession());
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (locator.isRootLocation()) {
			return createRootDirectory(locator, session);
		}

		try {
			final Path filePath = getEncryptedFilePath(locator.getResourcePath(), false);
			final Path dirFilePath = getEncryptedDirectoryFilePath(locator.getResourcePath(), false);
			if (Files.exists(dirFilePath)) {
				return createDirectory(locator, session, dirFilePath);
			} else if (Files.exists(filePath)) {
				return createFile(locator, session, filePath);
			}
		} catch (NonExistingParentException e) {
			// return non-existing
		}
		return createNonExisting(locator, session);
	}

	DavResource createChildDirectoryResource(DavResourceLocator locator, DavSession session, Path existingDirectoryFile) throws DavException {
		return createDirectory(locator, session, existingDirectoryFile);
	}

	DavResource createChildFileResource(DavResourceLocator locator, DavSession session, Path existingFile) throws DavException {
		return createFile(locator, session, existingFile);
	}

	/**
	 * @return <code>true</code> if a partial response should be generated according to an If-Range precondition.
	 */
	private boolean isIfRangePreconditionFulfilled(String ifRangeHeader, Path filePath) throws DavException {
		if (ifRangeHeader == null) {
			// no header set -> fulfilled implicitly
			return true;
		} else {
			try {
				final FileTime expectedTime = FileTimeUtils.fromRfc1123String(ifRangeHeader);
				final FileTime actualTime = Files.getLastModifiedTime(filePath);
				return expectedTime.compareTo(actualTime) == 0;
			} catch (DateTimeParseException e) {
				throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Unsupported If-Range header: " + ifRangeHeader);
			} catch (IOException e) {
				throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			}
		}
	}

	/**
	 * @return <code>true</code> if and only if exactly one byte range has been requested.
	 */
	private boolean isRangeSatisfiable(String rangeHeader) {
		assert rangeHeader != null;
		if (!rangeHeader.startsWith(RANGE_BYTE_PREFIX)) {
			return false;
		}
		final String byteRangeSet = StringUtils.removeStartIgnoreCase(rangeHeader, RANGE_BYTE_PREFIX);
		final String[] byteRanges = StringUtils.split(byteRangeSet, RANGE_SET_SEP);
		if (byteRanges.length != 1) {
			return false;
		}
		return true;
	}

	/**
	 * Processes the given range header field, if it is supported. Only headers containing a single byte range are supported.<br/>
	 * <code>
	 * bytes=100-200<br/>
	 * bytes=-500<br/>
	 * bytes=1000-
	 * </code>
	 * 
	 * @return Tuple of left and right range.
	 * @throws DavException HTTP statuscode 400 for malformed requests.
	 * @throws IllegalArgumentException If the given rangeHeader is not satisfiable. Check with {@link #isRangeSatisfiable(String)} before.
	 */
	private Pair<String, String> getRequestRange(String rangeHeader) throws DavException {
		assert rangeHeader != null;
		if (!rangeHeader.startsWith(RANGE_BYTE_PREFIX)) {
			throw new IllegalArgumentException("Unsatisfiable range. Should have generated 416 resonse.");
		}
		final String byteRangeSet = StringUtils.removeStartIgnoreCase(rangeHeader, RANGE_BYTE_PREFIX);
		final String[] byteRanges = StringUtils.split(byteRangeSet, RANGE_SET_SEP);
		if (byteRanges.length != 1) {
			throw new IllegalArgumentException("Unsatisfiable range. Should have generated 416 resonse.");
		}
		final String byteRange = byteRanges[0];
		final String[] bytePos = StringUtils.splitPreserveAllTokens(byteRange, RANGE_SEP);
		if (bytePos.length != 2 || bytePos[0].isEmpty() && bytePos[1].isEmpty()) {
			throw new DavException(DavServletResponse.SC_BAD_REQUEST, "malformed range header: " + rangeHeader);
		}
		return new ImmutablePair<>(bytePos[0], bytePos[1]);
	}

	/**
	 * @return Absolute file path for a given cleartext file resourcePath.
	 * @throws NonExistingParentException If one ancestor of the encrypted path is missing
	 */
	Path getEncryptedFilePath(String relativeCleartextPath, boolean createNonExisting) throws NonExistingParentException {
		assert relativeCleartextPath.startsWith("/");
		final String parentCleartextPath = StringUtils.prependIfMissing(FilenameUtils.getPathNoEndSeparator(relativeCleartextPath), "/");
		final Path parent = getEncryptedDirectoryPath(parentCleartextPath, createNonExisting);
		final String cleartextFilename = FilenameUtils.getName(relativeCleartextPath);
		try {
			final String encryptedFilename = filenameTranslator.getEncryptedFilename(cleartextFilename);
			return parent.resolve(encryptedFilename);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @return Absolute file path for a given cleartext file resourcePath.
	 * @throws NonExistingParentException If one ancestor of the encrypted path is missing
	 */
	Path getEncryptedDirectoryFilePath(String relativeCleartextPath, boolean createNonExisting) throws NonExistingParentException {
		assert relativeCleartextPath.startsWith("/");
		final String parentCleartextPath = StringUtils.prependIfMissing(FilenameUtils.getPathNoEndSeparator(relativeCleartextPath), "/");
		final Path parent = getEncryptedDirectoryPath(parentCleartextPath, createNonExisting);
		final String cleartextFilename = FilenameUtils.getName(relativeCleartextPath);
		try {
			final String encryptedFilename = filenameTranslator.getEncryptedDirFileName(cleartextFilename);
			return parent.resolve(encryptedFilename);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * @param createNonExisting if <code>false</code>, a {@link NonExistingParentException} will be thrown for missing ancestors.
	 * @return Absolute directory path for a given cleartext directory resourcePath.
	 * @throws NonExistingParentException if one ancestor directory is missing.
	 */
	private Path getEncryptedDirectoryPath(String relativeCleartextPath, boolean createNonExisting) throws NonExistingParentException {
		assert relativeCleartextPath.startsWith("/");
		assert "/".equals(relativeCleartextPath) || !relativeCleartextPath.endsWith("/");
		try {
			final Path result;
			if ("/".equals(relativeCleartextPath)) {
				// root level
				final String fixedRootDirectory = cryptor.encryptDirectoryPath("", FileSystems.getDefault().getSeparator());
				result = dataRoot.resolve(fixedRootDirectory);
			} else {
				final String parentCleartextPath = StringUtils.prependIfMissing(FilenameUtils.getPathNoEndSeparator(relativeCleartextPath), "/");
				final Path parent = getEncryptedDirectoryPath(parentCleartextPath, createNonExisting);
				final String cleartextFilename = FilenameUtils.getName(relativeCleartextPath);
				final String encryptedFilename = filenameTranslator.getEncryptedDirFileName(cleartextFilename);
				final Path directoryFile = parent.resolve(encryptedFilename);
				if (!createNonExisting && !Files.exists(directoryFile)) {
					throw new NonExistingParentException();
				}
				final String directoryId = filenameTranslator.getDirectoryId(directoryFile, true);
				final String directory = cryptor.encryptDirectoryPath(directoryId, FileSystems.getDefault().getSeparator());
				result = dataRoot.resolve(directory);
			}
			Files.createDirectories(result);
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private EncryptedFile createFilePart(DavResourceLocator locator, DavSession session, Pair<String, String> requestRange, Path filePath) {
		return new EncryptedFilePart(this, locator, session, requestRange, lockManager, cryptor, cryptoWarningHandler, filePath);
	}

	private EncryptedFile createFile(DavResourceLocator locator, DavSession session, Path filePath) {
		return new EncryptedFile(this, locator, session, lockManager, cryptor, cryptoWarningHandler, filePath);
	}

	private EncryptedDir createRootDirectory(DavResourceLocator locator, DavSession session) throws DavException {
		final Path rootFile = dataRoot.resolve(ROOT_FILE);
		final Path rootDir = filenameTranslator.getEncryptedDirectoryPath("");
		try {
			// make sure, root dir always exists.
			// create dir first (because it fails silently, if alreay existing)
			Files.createDirectories(rootDir);
			Files.createFile(rootFile);
		} catch (FileAlreadyExistsException e) {
			// no-op
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return createDirectory(locator, session, dataRoot.resolve(ROOT_FILE));
	}

	private EncryptedDir createDirectory(DavResourceLocator locator, DavSession session, Path filePath) {
		return new EncryptedDir(this, locator, session, lockManager, cryptor, filenameTranslator, filePath);
	}

	private NonExistingNode createNonExisting(DavResourceLocator locator, DavSession session) {
		return new NonExistingNode(this, locator, session, lockManager, cryptor);
	}
	
	static class NonExistingParentException extends Exception {
		
		private static final long serialVersionUID = 4421121746624627094L;
		
	}

}
