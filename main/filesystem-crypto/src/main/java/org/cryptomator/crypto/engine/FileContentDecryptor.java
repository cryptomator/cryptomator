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
public interface FileContentDecryptor extends Destroyable, Closeable {

	/**
	 * @return Number of bytes of the decrypted file.
	 */
	long contentLength();

	/**
	 * Appends further ciphertext to this decryptor. This method might block until space becomes available. If so, it is interruptable.
	 * 
	 * @param cleartext Cleartext data or {@link FileContentCryptor#EOF} to indicate the end of a ciphertext.
	 * @see #skipToPosition(long)
	 */
	void append(ByteBuffer ciphertext) throws InterruptedException;

	/**
	 * Returns the next decrypted cleartext in byte-by-byte FIFO order, meaning in the order ciphertext has been appended to this encryptor.
	 * However the number and size of the cleartext byte buffers doesn't need to resemble the ciphertext buffers.
	 * 
	 * This method might block if no cleartext is available yet.
	 * 
	 * @return Decrypted cleartext or {@link FileContentCryptor#EOF}.
	 */
	ByteBuffer cleartext() throws InterruptedException, AuthenticationFailedException;

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
