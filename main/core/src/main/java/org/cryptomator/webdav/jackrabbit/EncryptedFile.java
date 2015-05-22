/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.webdav.exceptions.IORuntimeException;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EncryptedFile extends AbstractEncryptedNode implements FileConstants {

	private static final Logger LOG = LoggerFactory.getLogger(EncryptedFile.class);

	protected final CryptoWarningHandler cryptoWarningHandler;

	public EncryptedFile(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor, CryptoWarningHandler cryptoWarningHandler, Path filePath) {
		super(factory, locator, session, lockManager, cryptor, filePath);
		if (filePath == null) {
			throw new IllegalArgumentException("filePath must not be null");
		}
		this.cryptoWarningHandler = cryptoWarningHandler;
		if (Files.isRegularFile(filePath)) {
			try (final FileChannel c = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.DSYNC); final FileLock lock = c.tryLock(0L, FILE_HEADER_LENGTH, true)) {
				final Long contentLength = cryptor.decryptedContentLength(c);
				properties.add(new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, contentLength));
				if (contentLength > RANGE_REQUEST_LOWER_LIMIT) {
					properties.add(new HttpHeaderProperty(HttpHeader.ACCEPT_RANGES.asString(), HttpHeaderValue.BYTES.asString()));
				}
			} catch (OverlappingFileLockException e) {
				// file header currently locked, report -1 for unknown size.
				properties.add(new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, -1l));
			} catch (IOException e) {
				LOG.error("Error reading filesize " + filePath.toString(), e);
				throw new IORuntimeException(e);
			} catch (MacAuthenticationFailedException e) {
				LOG.warn("Content length couldn't be determined due to MAC authentication violation.");
				// don't add content length DAV property
			}
		}
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		throw new UnsupportedOperationException("Can not add member to file.");
	}

	@Override
	public DavResourceIterator getMembers() {
		throw new UnsupportedOperationException("Can not list members of file.");
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		throw new UnsupportedOperationException("Can not remove member to file.");
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		if (Files.isRegularFile(filePath)) {
			outputContext.setModificationTime(Files.getLastModifiedTime(filePath).toMillis());
			outputContext.setProperty(HttpHeader.ACCEPT_RANGES.asString(), HttpHeaderValue.BYTES.asString());
			try (final SeekableByteChannel channel = Files.newByteChannel(filePath, StandardOpenOption.READ)) {
				final Long contentLength = cryptor.decryptedContentLength(channel);
				if (contentLength != null) {
					outputContext.setContentLength(contentLength);
				}
				if (outputContext.hasStream()) {
					cryptor.decryptFile(channel, outputContext.getOutputStream());
				}
			} catch (EOFException e) {
				LOG.warn("Unexpected end of stream (possibly client hung up).");
			} catch (MacAuthenticationFailedException e) {
				cryptoWarningHandler.macAuthFailed(getLocator().getResourcePath());
			} catch (DecryptFailedException e) {
				throw new IOException("Error decrypting file " + filePath.toString(), e);
			}
		}
	}

	@Override
	public void move(AbstractEncryptedNode dest) throws DavException, IOException {
		final Path srcPath = filePath;
		final Path dstPath;
		if (dest instanceof NonExistingNode) {
			dstPath = ((NonExistingNode) dest).getFilePath();
		} else {
			dstPath = dest.filePath;
		}

		try {
			Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Override
	public void copy(AbstractEncryptedNode dest, boolean shallow) throws DavException, IOException {
		final Path srcPath = filePath;
		final Path dstPath;
		if (dest instanceof NonExistingNode) {
			dstPath = ((NonExistingNode) dest).getFilePath();
		} else {
			dstPath = dest.filePath;
		}

		try {
			Files.copy(srcPath, dstPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.copy(srcPath, dstPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
