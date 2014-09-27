/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav.jackrabbit;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.jackrabbit.webdav.AbstractLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;

import de.sebastianstenzel.oce.crypto.Cryptor;

public class WebDavLocatorFactory extends AbstractLocatorFactory {

	private final Path fsRoot;
	private final Cryptor cryptor;

	public WebDavLocatorFactory(String fsRoot, String httpRoot, Cryptor cryptor) {
		super(httpRoot);
		this.fsRoot = FileSystems.getDefault().getPath(fsRoot);
		this.cryptor = cryptor;
	}

	/**
	 * @return Encrypted absolute paths on the file system.
	 */
	@Override
	protected String getRepositoryPath(String resourcePath, String wspPath) {
		if (resourcePath == null) {
			return fsRoot.toString();
		}
		final String encryptedRepoPath = cryptor.encryptPath(resourcePath, FileSystems.getDefault().getSeparator().charAt(0), '/', null);
		return fsRoot.resolve(encryptedRepoPath).toString();
	}

	/**
	 * @return Decrypted path for use in URIs.
	 */
	@Override
	protected String getResourcePath(String repositoryPath, String wspPath) {
		final Path absRepoPath = FileSystems.getDefault().getPath(repositoryPath);
		if (fsRoot.equals(absRepoPath)) {
			return null;
		} else {
			final Path relativeRepositoryPath = fsRoot.relativize(absRepoPath);
			final String resourcePath = cryptor.decryptPath(relativeRepositoryPath.toString(), FileSystems.getDefault().getSeparator().charAt(0), '/', null);
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

}
