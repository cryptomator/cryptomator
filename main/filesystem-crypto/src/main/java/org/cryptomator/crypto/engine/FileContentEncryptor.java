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
public interface FileContentEncryptor extends Destroyable, Closeable {

	/**
	 * Creates the encrypted file header. This header might depend on the already encrypted data,
	 * thus the caller should make sure all data is processed before requesting the header.
	 * 
	 * @return Encrypted file header.
	 */
	ByteBuffer getHeader();

	/**
	 * @return the size of headers created by this {@code FileContentCryptor}. The length of headers returned by {@link #getHeader()} equals this value.
	 */
	int getHeaderSize();

	/**
	 * Appends further cleartext to this encryptor. This method might block until space becomes available.
	 * 
	 * @param cleartext Cleartext data or {@link FileContentCryptor#EOF} to indicate the end of a cleartext.
	 */
	void append(ByteBuffer cleartext) throws InterruptedException;

	/**
	 * Cancels encryption due to an exception in the thread responsible for appending cleartext.
	 * The exception will be the root cause of an {@link UncheckedIOException} thrown by {@link #ciphertext()} when retrieving the encrypted result.
	 * 
	 * @param cause The exception making it impossible to {@link #append(ByteBuffer)} further cleartext.
	 */
	void cancelWithException(Exception cause) throws InterruptedException;

	/**
	 * Returns the next ciphertext in byte-by-byte FIFO order, meaning in the order cleartext has been appended to this encryptor.
	 * However the number and size of the ciphertext byte buffers doesn't need to resemble the cleartext buffers.
	 * 
	 * This method might block if no ciphertext is available yet.
	 * 
	 * @return Encrypted ciphertext of {@link FileContentCryptor#EOF}.
	 * @throws UncheckedIOException In case of I/O exceptions, e.g. caused by previous {@link #cancelWithException(Exception)}.
	 */
	ByteBuffer ciphertext() throws InterruptedException, UncheckedIOException;

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
