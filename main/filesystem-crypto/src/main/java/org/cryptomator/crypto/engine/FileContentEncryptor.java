/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import java.io.Closeable;
import java.nio.ByteBuffer;

import javax.security.auth.Destroyable;

/**
 * Stateful, thus not thread-safe.
 */
public interface FileContentEncryptor extends Destroyable, Closeable {

	/**
	 * Creates the encrypted file header. This header might depend on the already encrypted data,
	 * thus the caller should make sure all data is processed before requesting the header.
	 * 
	 * @return Encrypted file header.
	 */
	ByteBuffer getHeader();

	/**
	 * Appends further cleartext to this encryptor. This method might block until space becomes available.
	 * 
	 * @param cleartext Cleartext data or {@link FileContentCryptor#EOF} to indicate the end of a cleartext.
	 */
	void append(ByteBuffer cleartext) throws InterruptedException;

	/**
	 * Returns the next ciphertext in byte-by-byte FIFO order, meaning in the order cleartext has been appended to this encryptor.
	 * However the number and size of the ciphertext byte buffers doesn't need to resemble the cleartext buffers.
	 * 
	 * This method might block if no ciphertext is available yet.
	 * 
	 * @return Encrypted ciphertext of {@link FileContentCryptor#EOF}.
	 */
	ByteBuffer ciphertext() throws InterruptedException;

	/**
	 * Clears file-specific sensitive information.
	 */
	@Override
	void destroy();

	@Override
	default void close() {
		this.destroy();
	}

}
