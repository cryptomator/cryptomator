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
	void append(ByteBuffer cleartext);

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
	 * Calculates the cleartext bytes required to perform a partial encryption of a specific cleartext byte range.
	 * If this decryptor doesn't support partial encryption the result will be <code>[0, {@link Long#MAX_VALUE}]</code>.
	 * 
	 * @param cleartextRange The cleartext range the caller wants to ecnrypt.
	 * @return The cleartext range required in order to encrypt the given cleartext range.
	 */
	ByteRange cleartextRequiredToEncryptRange(ByteRange cleartextRange);

	/**
	 * Informs the encryptor, what the first byte of the next cleartext block will be. This method needs to be called only for partial encryption.
	 * 
	 * @param nextCleartextByte The first byte of the next cleartext buffer given via {@link #append(ByteBuffer)}.
	 * @throws IllegalArgumentException If nextCleartextByte is an invalid starting point. Only start bytes determined by {@link #cleartextRequiredToEncryptRange(ByteRange)} are supported.
	 */
	void skipToPosition(long nextCleartextByte) throws IllegalArgumentException;

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
