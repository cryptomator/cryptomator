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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Date;

import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.exceptions.XAApplicationException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class FsWebdavResourceHandler extends EnhancedWebdavStore<FsWebdavTransaction> {

	private static final Logger LOG = LoggerFactory.getLogger(FsWebdavResourceHandler.class);
	private static final String XA_SYS_DIR_PREFIX = "oce-webdav";
	private static final Path XA_SYS_DIR;
	
	static {
		final String tmpDirName = (String) System.getProperties().get("java.io.tmpdir");
		final Path tmpDirPath = FileSystems.getDefault().getPath(tmpDirName);
		try {
			XA_SYS_DIR = Files.createTempDirectory(tmpDirPath, XA_SYS_DIR_PREFIX);
		} catch (IOException e) {
			throw new IllegalStateException("Can't create tmp directory at " + tmpDirPath.toString());
		}
	}

	private final XAFileSystem xafs;
	private final String workingDirectory;
	private final FsWebdavCryptoAdapter cryptoAdapter;
	
	public FsWebdavResourceHandler(final File root) {
		super(FsWebdavTransaction.class);
		this.workingDirectory = FilenameUtils.normalizeNoEndSeparator(root.getAbsolutePath());
		
		final StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XA_SYS_DIR.toString(), "test");
        this.xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
        this.cryptoAdapter = new FsWebdavCryptoAdapter(this.workingDirectory);
		
        try {
        	this.xafs.waitForBootup(1000L);
        	LOG.info("Started XADisk at " + XA_SYS_DIR.toString());
        	
        	final Session session = xafs.createSessionForLocalTransaction();
        	cryptoAdapter.initializeNewFolder(session, "/");
        	session.commit();
		} catch (IOException | XAApplicationException | InterruptedException ex) {
			throw new IllegalStateException("Could not initialize I/O components.", ex);
		}
	}
	
	private File getFileInWorkDir(final String relativeUri) {
		final String fullPath = this.workingDirectory.concat(relativeUri);
		return new File(FilenameUtils.normalize(fullPath));
	}
	
	@Override
	public void destroy() {
		try {
			this.xafs.shutdown();
			FileUtils.deleteDirectory(XA_SYS_DIR.toFile());
		} catch (IOException e) {
			LOG.warn("Failed to shutdown normally", e);
		}
	}

	@Override
	public FsWebdavTransaction beginTransactionInternal(Principal principal) {
		final Session session = this.xafs.createSessionForLocalTransaction();
		LOG.trace("started transaction " + session);
		return new FsWebdavTransaction(principal, session);
	}

	@Override
	public void checkAuthenticationInternal(FsWebdavTransaction transaction) {
		// TODO Auto-generated method stub
	}

	@Override
	public void commitInternal(FsWebdavTransaction transaction) {
		try {
			transaction.getSession().commit();
			LOG.trace("committed transaction " + transaction.getSession());
		} catch (NoTransactionAssociatedException e) {
			throw new WebdavException("Error committing transaction " + transaction.getSession(), e);
		}
	}

	@Override
	public void rollbackInternal(FsWebdavTransaction transaction) {
		try {
			transaction.getSession().rollback();
			LOG.warn("rolled back transaction " + transaction.getSession());
		} catch (NoTransactionAssociatedException e) {
			throw new WebdavException("Error rolling back transaction " + transaction.getSession(), e);
		}
	}

	@Override
	public void createFolderInternal(FsWebdavTransaction transaction, String folderUri) {
		try {
			cryptoAdapter.initializeNewFolder(transaction.getSession(), folderUri);
		} catch (IOException e) {
			throw new WebdavException(e);
		}
	}

	@Override
	public void createResourceInternal(FsWebdavTransaction transaction, String resourceUri) {
		try {			
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), resourceUri);
			final File file = getFileInWorkDir(pseudonymized);
			transaction.getSession().createFile(file, false);
		} catch (IOException | XAApplicationException | InterruptedException e) {
			throw new WebdavException(e);
		}
	}

	@Override
	public InputStream getResourceContentInternal(FsWebdavTransaction transaction, String resourceUri) {
		try {
			// Note: The requesting entity is in charge of closing the stream.
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), resourceUri);
			return cryptoAdapter.decryptResource(transaction.getSession(), pseudonymized);
		} catch (IOException e) {
			throw new WebdavException(e);
		}
	}

	@Override
	public long setResourceContentInternal(FsWebdavTransaction transaction, String resourceUri, InputStream in, String contentType, String characterEncoding) {
		try {
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), resourceUri);
			return cryptoAdapter.encryptResource(transaction.getSession(), pseudonymized, in);
		} catch (IOException  e) {
			throw new WebdavException(e);
		}
	}
	
	@Override
	public String[] getChildrenNamesInternal(FsWebdavTransaction transaction, String folderUri) {
		try {
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), folderUri);
			return cryptoAdapter.uncoveredChildrenNames(transaction.getSession(), pseudonymized);
		} catch (IOException e) {
			throw new WebdavException(e);
		}
	}

	@Override
	public long getResourceLengthInternal(FsWebdavTransaction transaction, String uri) {
		try {
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), uri);
			return cryptoAdapter.getDecryptedFileLength(transaction.getSession(), pseudonymized);
		} catch (IOException e) {
			throw new WebdavException(e);
		}
	}

	@Override
	public void removeObjectInternal(FsWebdavTransaction transaction, String uri) {
		try {
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), uri);
			final File file = getFileInWorkDir(pseudonymized);
			deleteRecursively(transaction.getSession(), file);
			cryptoAdapter.deletePseudonym(transaction.getSession(), pseudonymized);
		} catch (IOException | XAApplicationException | InterruptedException e) {
			LOG.error("removeObject" + uri + " failed", e);
			throw new WebdavException(e);
		}
	}
	
	private void deleteRecursively(Session session, File file) throws XAApplicationException, InterruptedException {
		if (file.isDirectory()) {
			final String[] children = session.listFiles(file);
			for (final String childName : children) {
				final File childFile = new File(file, childName);
				deleteRecursively(session, childFile);
			}
		}
		session.deleteFile(file);
	}

	@Override
	public StoredObject getStoredObjectInternal(FsWebdavTransaction transaction, String uri) {
		try {
			final String pseudonymized = cryptoAdapter.pseudonymizedUri(transaction.getSession(), uri);
			final File file = getFileInWorkDir(pseudonymized);
			if (transaction.getSession().fileExists(file)) {
				final StoredObject so = new StoredObject();
				so.setFolder(file.isDirectory());
				so.setLastModified(new Date(file.lastModified()));
				so.setCreationDate(new Date(file.lastModified()));
				if (!file.isDirectory()) {
					so.setResourceLength(transaction.getSession().getFileLength(file));
				}
				return so;
			} else {
				return null;
			}
		} catch (IOException | XAApplicationException | InterruptedException e) {
			throw new WebdavException(e);
		}
	}

}
