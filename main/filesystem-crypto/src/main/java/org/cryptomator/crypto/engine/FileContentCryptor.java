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
	 * @param header The full fixed-length header of an encrypted file. The caller is required to pass the exact amount of bytes returned by {@link #getHeaderSize()}.
	 * @return A possibly new FileContentDecryptor instance which is capable of decrypting ciphertexts associated with the given file header.
	 */
	FileContentDecryptor createFileContentDecryptor(ByteBuffer header);

	/**
	 * @param header The full fixed-length header of an encrypted file or {@link Optional#empty()}. The caller is required to pass the exact amount of bytes returned by {@link #getHeaderSize()}.
	 *            If the header is empty, a new one will be created by the returned encryptor.
	 * @return A possibly new FileContentEncryptor instance which is capable of encrypting cleartext associated with the given file header.
	 */
	FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header);

}
