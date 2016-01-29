/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blacklisting;

import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.delegating.DelegatingFolder;

class BlacklistingFolder extends DelegatingFolder<BlacklistingFolder, BlacklistingFile> {

	private final Predicate<Node> hiddenNodes;

	public BlacklistingFolder(BlacklistingFolder parent, Folder delegate, Predicate<Node> hiddenNodes) {
		super(parent, delegate);
		this.hiddenNodes = hiddenNodes;
	}

	@Override
	public Stream<? extends Node> children() {
		return Stream.concat(folders(), files());
	}

	@Override
	public Stream<BlacklistingFolder> folders() {
		return delegate.folders().filter(hiddenNodes.negate()).map(this::newFolder);
	}

	@Override
	public Stream<BlacklistingFile> files() {
		return delegate.files().filter(hiddenNodes.negate()).map(this::newFile);
	}

	@Override
	protected BlacklistingFile newFile(File delegate) {
		if (hiddenNodes.test(delegate)) {
			throw new UncheckedIOException("'" + delegate.name() + "' is a reserved name.", new FileAlreadyExistsException(delegate.name()));
		}
		return new BlacklistingFile(this, delegate);
	}

	@Override
	protected BlacklistingFolder newFolder(Folder delegate) {
		if (hiddenNodes.test(delegate)) {
			throw new UncheckedIOException("'" + delegate.name() + "' is a reserved name.", new FileAlreadyExistsException(delegate.name()));
		}
		return new BlacklistingFolder(this, delegate, hiddenNodes);
	}

}
