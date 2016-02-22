/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Factory for stateful {@link FileContentEncryptor Encryptor}/{@link FileContentDecryptor Decryptor} instances, that are capable of processing data exactly once.
 */
public interface FileContentCryptor {

	public static final ByteBuffer EOF = ByteBuffer.allocate(0);

	/**
	 * @return The fixed number of bytes of the file header. The header length is implementation-specific.
	 */
	int getHeaderSize();

	/**
	 * @return The ciphertext position that correlates to the cleartext position.
	 */
	long toCiphertextPos(long cleartextPos);

	/**
	 * @param header The full fixed-length header of an encrypted file. The caller is required to pass the exact amount of bytes returned by {@link #getHeaderSize()}.
	 * @param firstCiphertextByte Position of the first ciphertext byte passed to the decryptor. If the decryptor can not fast-forward to the requested byte, an exception is thrown.
	 *            If firstCiphertextByte is an invalid starting point, i.e. doesn't align with the decryptors internal block size, an IllegalArgumentException will be thrown.
	 * @param authenticate Skip authentication by setting this flag to <code>false</code>. Should be <code>true</code> by default.
	 * @return A possibly new FileContentDecryptor instance which is capable of decrypting ciphertexts associated with the given file header.
	 */
	FileContentDecryptor createFileContentDecryptor(ByteBuffer header, long firstCiphertextByte, boolean authenticate) throws IllegalArgumentException, AuthenticationFailedException;

	/**
	 * @param header The full fixed-length header of an encrypted file or {@link Optional#empty()}. The caller is required to pass the exact amount of bytes returned by {@link #getHeaderSize()}.
	 *            If the header is empty, a new one will be created by the returned encryptor.
	 * @param firstCleartextByte Position of the first cleartext byte passed to the encryptor. If the encryptor can not fast-forward to the requested byte, an exception is thrown.
	 *            If firstCiphertextByte is an invalid starting point, i.e. doesn't align with the encryptors internal block size, an IllegalArgumentException will be thrown.
	 * @return A possibly new FileContentEncryptor instance which is capable of encrypting cleartext associated with the given file header.
	 */
	FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header, long firstCleartextByte) throws IllegalArgumentException;

}
