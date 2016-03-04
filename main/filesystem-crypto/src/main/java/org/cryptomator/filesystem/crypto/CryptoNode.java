/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

abstract class CryptoNode implements Node {

	protected final CryptoFolder parent;
	protected final String name;
	protected final Cryptor cryptor;

	public CryptoNode(CryptoFolder parent, String name, Cryptor cryptor) {
		this.parent = parent;
		this.name = name;
		this.cryptor = cryptor;
	}

	protected Folder physicalDataRoot() {
		return parent.physicalDataRoot();
	}

	protected abstract Optional<String> encryptedName();

	protected Optional<File> physicalFile() {
		if (parent.exists() && encryptedName().isPresent()) {
			return Optional.of(parent.forceGetPhysicalFolder().file(encryptedName().get()));
		} else {
			return Optional.empty();
		}
	}

	protected File forceGetPhysicalFile() {
		return physicalFile().orElseThrow(() -> {
			return new UncheckedIOException(new FileNotFoundException(toString()));
		});
	}

	@Override
	public CryptoFileSystem fileSystem() {
		return (CryptoFileSystem) Node.super.fileSystem();
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.of(parent);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean exists() {
		return physicalFile().map(File::exists).orElse(false);
	}

	@Override
	public Instant lastModified() {
		return forceGetPhysicalFile().lastModified();
	}

	@Override
	public void setLastModified(Instant lastModified) throws UncheckedIOException {
		forceGetPhysicalFile().setLastModified(lastModified);
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		return forceGetPhysicalFile().creationTime();
	}

	@Override
	public void setCreationTime(Instant creationTime) throws UncheckedIOException {
		forceGetPhysicalFile().setCreationTime(creationTime);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CryptoNode) {
			CryptoNode other = (CryptoNode) obj;
			return this.getClass() == other.getClass() //
					&& (this.parent == null && other.parent == null || this.parent != null && this.parent.equals(other.parent)) //
					&& (this.name == null && other.name == null || this.name != null && this.name.equals(other.name));
		} else {
			return false;
		}
	}

	/**
	 * Unix-style cleartext path rooted at the vault's top-level directory.
	 * 
	 * @return Vault-relative cleartext path.
	 */
	@Override
	public abstract String toString();

}
