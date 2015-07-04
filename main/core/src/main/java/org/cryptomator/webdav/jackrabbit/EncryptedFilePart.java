package org.cryptomator.webdav.jackrabbit;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers only the requested range of bytes from a file.
 * 
 * @see {@link https://tools.ietf.org/html/rfc7233#section-4}
 */
class EncryptedFilePart extends EncryptedFile {

	private static final Logger LOG = LoggerFactory.getLogger(EncryptedFilePart.class);

	private final Pair<Long, Long> range;

	public EncryptedFilePart(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, Pair<String, String> requestRange, LockManager lockManager, Cryptor cryptor,
			CryptoWarningHandler cryptoWarningHandler, Path filePath) {
		super(factory, locator, session, lockManager, cryptor, cryptoWarningHandler, filePath);

		try {
			final Long lower = requestRange.getLeft().isEmpty() ? null : Long.valueOf(requestRange.getLeft());
			final Long upper = requestRange.getRight().isEmpty() ? null : Long.valueOf(requestRange.getRight());
			if (lower == null) {
				range = new ImmutablePair<Long, Long>(contentLength - upper, contentLength - 1);
			} else if (upper == null) {
				range = new ImmutablePair<Long, Long>(lower, contentLength - 1);
			} else {
				range = new ImmutablePair<Long, Long>(lower, upper);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid byte range: " + requestRange, e);
		}
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		assert Files.isRegularFile(filePath);
		assert this.contentLength != null;

		final Long rangeLength = range.getRight() - range.getLeft() + 1;
		outputContext.setContentLength(rangeLength);
		outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), getContentRangeHeader(range.getLeft(), range.getRight(), contentLength));
		outputContext.setModificationTime(Files.getLastModifiedTime(filePath).toMillis());

		try (final FileChannel c = FileChannel.open(filePath, StandardOpenOption.READ)) {
			if (outputContext.hasStream()) {
				cryptor.decryptRange(c, outputContext.getOutputStream(), range.getLeft(), rangeLength);
			}
		} catch (EOFException e) {
			if (LOG.isDebugEnabled()) {
				LOG.trace("Unexpected end of stream during delivery of partial content (client hung up).");
			}
		} catch (MacAuthenticationFailedException e) {
			LOG.warn("File integrity violation for " + getLocator().getResourcePath());
			cryptoWarningHandler.macAuthFailed(getLocator().getResourcePath());
			throw new IOException("Error decrypting file " + filePath.toString(), e);
		} catch (DecryptFailedException e) {
			throw new IOException("Error decrypting file " + filePath.toString(), e);
		}
	}

	private String getContentRangeHeader(long firstByte, long lastByte, long completeLength) {
		return String.format("bytes %d-%d/%d", firstByte, lastByte, completeLength);
	}

}
