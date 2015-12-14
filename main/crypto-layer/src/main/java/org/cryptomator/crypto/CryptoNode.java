package org.cryptomator.crypto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

abstract class CryptoNode implements Node {

	protected final CryptoFolder parent;
	protected final String name;

	public CryptoNode(CryptoFolder parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	Folder physicalDataRoot() {
		return parent.physicalDataRoot();
	}

	Folder physicalMetadataRoot() {
		return parent.physicalMetadataRoot();
	}

	@Override
	public Optional<CryptoFolder> parent() {
		return Optional.of(parent);
	}

	@Override
	public String name() {
		return name;
	}

	String encryptedName() {
		return name();
	}

	@Override
	public boolean exists() {
		try {
			return parent.children().anyMatch(node -> node.equals(this));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
					&& (this.parent == null && other.parent == null || this.parent.equals(other.parent)) //
					&& (this.name == null && other.name == null || this.name.equals(other.name));
		} else {
			return false;
		}
	}

}
