package org.cryptomator.crypto.engine;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/**
 * Stateful, thus not thread-safe.
 */
public interface FileContentDecryptor {

	public static final ByteBuffer EOF = ByteBuffer.allocate(0);

	/**
	 * @return Number of bytes of the decrypted file.
	 */
	long contentLength();

	/**
	 * Appends further ciphertext to this decryptor. This method might block until space becomes available. If so, it is interruptable.
	 * 
	 * @param cleartext Cleartext data or {@link #EOF} to indicate the end of a ciphertext.
	 * @see #skipToPosition(long)
	 */
	void append(ByteBuffer ciphertext);

	/**
	 * Returns a queue containing cleartext in byte-by-byte FIFO order, meaning in the order ciphertext has been appended to this encryptor.
	 * However the number and size of the ciphertext byte buffers doesn't need to resemble the ciphertext buffers.
	 * 
	 * The queue returns {@link #EOF}, when all ciphertext has been processed.
	 * 
	 * @return A queue from which decrypted data can be {@link BlockingQueue#take() taken}.
	 */
	BlockingQueue<ByteBuffer> cleartext();

	/**
	 * Calculates the ciphertext bytes required to perform a partial decryption of a requested cleartext byte range.
	 * If this decryptor doesn't support partial decryption the result will be <code>[0, {@link Long#MAX_VALUE}]</code>.
	 * 
	 * @param cleartextRange The cleartext range the caller is interested in.
	 * @return The ciphertext range required in order to decrypt the cleartext range.
	 */
	ByteRange ciphertextRequiredToDecryptRange(ByteRange cleartextRange);

	/**
	 * Informs the decryptor, what the first byte of the next ciphertext block will be. This method needs to be called only for partial decryption.
	 * 
	 * @param nextCiphertextByte The first byte of the next ciphertext buffer given via {@link #append(ByteBuffer)}.
	 * @throws IllegalArgumentException If nextCiphertextByte is an invalid starting point. Only start bytes determined by {@link #ciphertextRequiredToDecryptRange(ByteRange)} are supported.
	 */
	void skipToPosition(long nextCiphertextByte) throws IllegalArgumentException;

}
