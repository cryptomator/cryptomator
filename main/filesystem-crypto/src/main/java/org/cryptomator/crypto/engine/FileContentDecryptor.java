/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import java.io.Closeable;
import java.io.UncheckedIOException;
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
	 * Cancels decryption due to an exception in the thread responsible for appending ciphertext.
	 * The exception will be the root cause of an {@link UncheckedIOException} thrown by {@link #cleartext()} when retrieving the decrypted result.
	 * 
	 * @param cause The exception making it impossible to {@link #append(ByteBuffer)} further ciphertext.
	 */
	void cancelWithException(Exception cause) throws InterruptedException;

	/**
	 * Returns the next decrypted cleartext in byte-by-byte FIFO order, meaning in the order ciphertext has been appended to this encryptor.
	 * However the number and size of the cleartext byte buffers doesn't need to resemble the ciphertext buffers.
	 * 
	 * This method might block if no cleartext is available yet.
	 * 
	 * @return Decrypted cleartext or {@link FileContentCryptor#EOF}.
	 * @throws AuthenticationFailedException On MAC mismatches
	 * @throws UncheckedIOException In case of I/O exceptions, e.g. caused by previous {@link #cancelWithException(Exception)}.
	 */
	ByteBuffer cleartext() throws InterruptedException, AuthenticationFailedException, UncheckedIOException;

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
