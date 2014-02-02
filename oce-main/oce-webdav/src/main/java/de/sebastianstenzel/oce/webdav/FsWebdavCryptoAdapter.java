/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xadisk.additional.XAFileInputStreamWrapper;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.exceptions.XAApplicationException;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.TransactionAwareFileAccess;
import de.sebastianstenzel.oce.crypto.aes256.AesCryptor;

final class FsWebdavCryptoAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(FsWebdavCryptoAdapter.class);
	private final Cryptor cryptor = new AesCryptor();
	private final Path workDir;

	public FsWebdavCryptoAdapter(final String workingDirectory) {
		this.workDir = FileSystems.getDefault().getPath(workingDirectory);
	}

	/**
	 * Creates a new folder and initializes its metadata file.
	 * 
	 * @return The pseudonymized URI of the created folder.
	 */
	public String initializeNewFolder(final Session session, final String clearUri) throws IOException {
		final String pseudonymized = this.pseudonymizedUri(session, clearUri);
		final TransactionAwareFileAccess accessor = new FileLoader(session);
		final File folder = accessor.resolveUri(pseudonymized).toFile();
		try {
			if (!session.fileExistsAndIsDirectory(folder)) {
				session.createFile(folder, true);
			}
		} catch (NoTransactionAssociatedException ex) {
			throw new IllegalStateException("Session closed.", ex);
		} catch (XAApplicationException | InterruptedException ex) {
			throw new IOException(ex);
		}
		return pseudonymized;
	}

	/**
	 * @return List of all cleartext child resource names for the directory with
	 *         the given URI.
	 */
	public String[] uncoveredChildrenNames(final Session session, final String pseudonymizedUri) throws IOException {
		try {
			final TransactionAwareFileAccess accessor = new FileLoader(session);
			final File file = accessor.resolveUri(pseudonymizedUri).toFile();
			final List<String> result = new ArrayList<>();
			if (file.isDirectory()) {
				String[] children = session.listFiles(file);
				for (final String child : children) {
					final String pseudonym = FilenameUtils.concat(pseudonymizedUri, child);
					final String cleartext = cryptor.uncoverPseudonym(pseudonym, accessor);
					if (cleartext != null) {
						result.add(FilenameUtils.getName(cleartext));
					}
				}
			}
			return result.toArray(new String[result.size()]);
		} catch (XAApplicationException | InterruptedException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @return The pseudonyimzed URI for the given clear URI.
	 */
	public String pseudonymizedUri(final Session session, final String clearUri) throws IOException {
		final TransactionAwareFileAccess fileLoader = new FileLoader(session);
		return cryptor.createPseudonym(clearUri, fileLoader);
	}

	/**
	 * Deletes a pseudonym.
	 */
	public void deletePseudonym(final Session session, final String pseudonymizedUri) throws IOException {
		final TransactionAwareFileAccess fileLoader = new FileLoader(session);
		cryptor.deletePseudonym(pseudonymizedUri, fileLoader);
	}
	
	public InputStream decryptResource(Session session, String pseudonymized) throws IOException {
		final TransactionAwareFileAccess accessor = new FileLoader(session);
		return cryptor.decryptFile(pseudonymized, accessor);
	}
	
	public long encryptResource(Session session, String pseudonymized, InputStream in) throws IOException {
		final TransactionAwareFileAccess accessor = new FileLoader(session);
		return cryptor.encryptFile(pseudonymized, in, accessor);
	}
	

	public long getDecryptedFileLength(Session session, String pseudonymized) throws IOException {
		final TransactionAwareFileAccess accessor = new FileLoader(session);
		return cryptor.getDecryptedContentLength(pseudonymized, accessor);
	}


	/**
	 * Transaction-aware implementation of MetadataLoading.
	 */
	private class FileLoader implements TransactionAwareFileAccess {

		private final Session session;

		private FileLoader(final Session session) {
			this.session = session;
		}

		@Override
		public InputStream openFileForRead(Path path) throws IOException {
			try {
				final File file = path.toFile();
				if (!session.fileExists(file)) {
					session.createFile(file, false);
				}
				return new XAFileInputStreamWrapper(session.createXAFileInputStream(file));
			} catch (XAApplicationException | InterruptedException ex) {
				LOG.error("Failed to open resource for reading: " + path.toString(), ex);
				throw new IOException("Failed to open resource for reading: " + path.toString(), ex);
			}
		}

		@Override
		public OutputStream openFileForWrite(Path path) throws IOException {
			try {
				final File file = path.toFile();
				if (!session.fileExists(file)) {
					session.createFile(file, false);
				} else {
					session.truncateFile(file, 0);
				}
				return new XAFileOutputStreamWrapper(session.createXAFileOutputStream(file, false));
			} catch (NoTransactionAssociatedException ex) {
				LOG.error("Session closed.", ex);
				throw new IllegalStateException("Session closed.", ex);
			} catch (XAApplicationException | InterruptedException ex) {
				LOG.error("Failed to open resource for writing: " + path.toString(), ex);
				throw new IOException("Failed to open resource for writing: " + path.toString(), ex);
			}
		}
		
		@Override
		public Path resolveUri(String uri) {
			return workDir.resolve(removeLeadingSlash(uri));
		}
		
		private String removeLeadingSlash(String path) {
			if (path.length() == 0) {
				return path;
			} else if (path.charAt(0) == '/') {
				return path.substring(1);
			} else {
				return path;
			}
		}

	}

}
