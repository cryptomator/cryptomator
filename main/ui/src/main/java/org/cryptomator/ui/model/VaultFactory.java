/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import static java.util.UUID.randomUUID;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.filesystem.crypto.CryptoFileSystemFactory;
import org.cryptomator.filesystem.shortening.ShorteningFileSystemFactory;
import org.cryptomator.ui.util.DeferredCloser;

@Singleton
public class VaultFactory {

	private final ShorteningFileSystemFactory shorteningFileSystemFactory;
	private final CryptoFileSystemFactory cryptoFileSystemFactory;
	private final DeferredCloser closer;

	@Inject
	public VaultFactory(ShorteningFileSystemFactory shorteningFileSystemFactory, CryptoFileSystemFactory cryptoFileSystemFactory, DeferredCloser closer) {
		this.shorteningFileSystemFactory = shorteningFileSystemFactory;
		this.cryptoFileSystemFactory = cryptoFileSystemFactory;
		this.closer = closer;
	}

	public Vault createVault(String id, Path path) {
		return new Vault(id, path, shorteningFileSystemFactory, cryptoFileSystemFactory, closer);
	}

	public Vault createVault(Path path) {
		return createVault(generateId(), path);
	}

	private String generateId() {
		return asBase64String(nineBytesFrom(randomUUID()));
	}

	private String asBase64String(ByteBuffer bytes) {
		ByteBuffer base64Buffer = Base64.getUrlEncoder().encode(bytes);
		return new String(asByteArray(base64Buffer));
	}

	private ByteBuffer nineBytesFrom(UUID uuid) {
		ByteBuffer uuidBuffer = ByteBuffer.allocate(9);
		uuidBuffer.putLong(uuid.getMostSignificantBits());
		uuidBuffer.put((byte)(uuid.getLeastSignificantBits() & 0xFF));
		uuidBuffer.flip();
		return uuidBuffer;
	}

	private byte[] asByteArray(ByteBuffer buffer) {
		if (buffer.hasArray()) {
			return buffer.array();
		} else {
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			return bytes;
		}
	}

}
