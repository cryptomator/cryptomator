/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.collections4.BidiMap;
import org.apache.jackrabbit.webdav.AbstractLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.CryptorIOSupport;
import org.cryptomator.crypto.SensitiveDataSwipeListener;

class DavLocatorFactoryImpl extends AbstractLocatorFactory implements SensitiveDataSwipeListener, CryptorIOSupport {

	private static final int MAX_CACHED_PATHS = 10000;
	private final Path fsRoot;
	private final Cryptor cryptor;
	private final BidiMap<String, String> pathCache = new BidiLRUMap<>(MAX_CACHED_PATHS); // <decryptedPath, encryptedPath>

	DavLocatorFactoryImpl(String fsRoot, String httpRoot, Cryptor cryptor) {
		super(httpRoot);
		this.fsRoot = FileSystems.getDefault().getPath(fsRoot);
		this.cryptor = cryptor;
		cryptor.addSensitiveDataSwipeListener(this);
	}

	/**
	 * @return Encrypted absolute paths on the file system.
	 */
	@Override
	protected String getRepositoryPath(String resourcePath, String wspPath) {
		String encryptedPath = pathCache.get(resourcePath);
		if (encryptedPath == null) {
			encryptedPath = encryptRepositoryPath(resourcePath);
			pathCache.put(resourcePath, encryptedPath);
		}
		return encryptedPath;
	}

	private String encryptRepositoryPath(String resourcePath) {
		if (resourcePath == null) {
			return fsRoot.toString();
		}
		final String encryptedRepoPath = cryptor.encryptPath(resourcePath, FileSystems.getDefault().getSeparator().charAt(0), '/', this);
		return fsRoot.resolve(encryptedRepoPath).toString();
	}

	/**
	 * @return Decrypted path for use in URIs.
	 */
	@Override
	protected String getResourcePath(String repositoryPath, String wspPath) {
		String decryptedPath = pathCache.getKey(repositoryPath);
		if (decryptedPath == null) {
			decryptedPath = decryptResourcePath(repositoryPath);
			pathCache.put(decryptedPath, repositoryPath);
		}
		return decryptedPath;
	}

	private String decryptResourcePath(String repositoryPath) {
		final Path absRepoPath = FileSystems.getDefault().getPath(repositoryPath);
		if (fsRoot.equals(absRepoPath)) {
			return null;
		} else {
			final Path relativeRepositoryPath = fsRoot.relativize(absRepoPath);
			final String resourcePath = cryptor.decryptPath(relativeRepositoryPath.toString(), FileSystems.getDefault().getSeparator().charAt(0), '/', this);
			return resourcePath;
		}
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		// we don't support workspaces
		return super.createResourceLocator(prefix, "", path, isResourcePath);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		// we don't support workspaces
		return super.createResourceLocator(prefix, "", resourcePath);
	}

	@Override
	public void swipeSensitiveData() {
		pathCache.clear();
	}

	/* Cryptor I/O Support */

	@Override
	public void writePathSpecificMetadata(String encryptedPath, byte[] encryptedMetadata) throws IOException {
		final Path metaDataFile = fsRoot.resolve(encryptedPath);
		Files.write(metaDataFile, encryptedMetadata, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC);
	}

	@Override
	public byte[] readPathSpecificMetadata(String encryptedPath) throws IOException {
		final Path metaDataFile = fsRoot.resolve(encryptedPath);
		if (!Files.isReadable(metaDataFile)) {
			return null;
		} else {
			return Files.readAllBytes(metaDataFile);
		}
	}

}
