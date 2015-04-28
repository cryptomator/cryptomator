package org.cryptomator.webdav.jackrabbit;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import org.apache.commons.httpclient.HttpStatus;
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

public class CryptoResourceFactory implements DavResourceFactory {

	private final LockManager lockManager = new SimpleLockManager();
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;
	private final ExecutorService backgroundTaskExecutor;

	CryptoResourceFactory(Cryptor cryptor, CryptoWarningHandler cryptoWarningHandler, ExecutorService backgroundTaskExecutor) {
		this.cryptor = cryptor;
		this.cryptoWarningHandler = cryptoWarningHandler;
		this.backgroundTaskExecutor = backgroundTaskExecutor;
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		if (locator instanceof CryptoLocator) {
			return createResource((CryptoLocator) locator, request, response);
		} else {
			throw new IllegalArgumentException("Unsupported resource locator of type " + locator.getClass().getName());
		}
	}

	@Override
	public final DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (locator instanceof CryptoLocator) {
			return createResource((CryptoLocator) locator, session);
		} else {
			throw new IllegalArgumentException("Unsupported resource locator of type " + locator.getClass().getName());
		}
	}

	private DavResource createResource(CryptoLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		final Path filepath = FileSystems.getDefault().getPath(locator.getRepositoryPath());
		final Path dirpath = FileSystems.getDefault().getPath(locator.getDirectoryPath());
		final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());

		if (Files.isDirectory(dirpath) || DavMethods.METHOD_MKCOL.equals(request.getMethod())) {
			return createDirectory(locator, request.getDavSession());
		} else if (Files.isRegularFile(filepath) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null) {
			response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
			return createFilePart(locator, request.getDavSession(), request);
		} else if (Files.isRegularFile(filepath) || DavMethods.METHOD_PUT.equals(request.getMethod())) {
			return createFile(locator, request.getDavSession());
		} else {
			return createNonExisting(locator, request.getDavSession());
		}
	}

	private DavResource createResource(CryptoLocator locator, DavSession session) throws DavException {
		final Path filepath = FileSystems.getDefault().getPath(locator.getRepositoryPath());
		final Path dirpath = FileSystems.getDefault().getPath(locator.getDirectoryPath());

		if (Files.isDirectory(dirpath)) {
			return createDirectory(locator, session);
		} else if (Files.isRegularFile(filepath)) {
			return createFile(locator, session);
		} else {
			return createNonExisting(locator, session);
		}
	}

	private EncryptedFile createFilePart(CryptoLocator locator, DavSession session, DavServletRequest request) {
		return new EncryptedFilePart(this, locator, session, request, lockManager, cryptor, cryptoWarningHandler, backgroundTaskExecutor);
	}

	private EncryptedFile createFile(CryptoLocator locator, DavSession session) {
		return new EncryptedFile(this, locator, session, lockManager, cryptor, cryptoWarningHandler);
	}

	private EncryptedDir createDirectory(CryptoLocator locator, DavSession session) {
		return new EncryptedDir(this, locator, session, lockManager, cryptor);
	}

	private NonExistingNode createNonExisting(CryptoLocator locator, DavSession session) {
		return new NonExistingNode(this, locator, session, lockManager, cryptor);
	}

}
