/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.delegating.DelegatingFileSystem;

public class ShorteningFileSystem extends ShorteningFolder implements DelegatingFileSystem {

	private final String metadataFolderName;

	public ShorteningFileSystem(Folder root, String metadataFolderName, int threshold) {
		super(null, root, "", new FilenameShortener(root.resolveFolder(metadataFolderName), threshold));
		this.metadataFolderName = metadataFolderName;
		create();
	}

	@Override
	public Stream<ShorteningFolder> folders() {
		return super.folders().filter(this::nameIsNotNameOfMetadataFolder);
	}

	@Override
	public ShorteningFile file(String name) throws UncheckedIOException {
		if (metadataFolderName.equals(name)) {
			throw new UncheckedIOException(new IOException("'" + name + "' is a reserved name."));
		}
		return super.file(name);
	}

	@Override
	public ShorteningFolder folder(String name) throws UncheckedIOException {
		if (metadataFolderName.equals(name)) {
			throw new UncheckedIOException(new IOException("'" + name + "' is a reserved name."));
		}
		return super.folder(name);
	}

	private boolean nameIsNotNameOfMetadataFolder(Node node) {
		return !metadataFolderName.equals(node.name());
	}

	@Override
	public Folder getDelegate() {
		return delegate;
	}

}
