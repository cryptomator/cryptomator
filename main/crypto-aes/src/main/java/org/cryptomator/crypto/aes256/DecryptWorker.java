package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.crypto.Cipher;
import javax.crypto.Mac;

import org.cryptomator.crypto.exceptions.CryptingException;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;

abstract class DecryptWorker extends CryptoWorker implements AesCryptographicConfiguration {

	private final boolean shouldAuthenticate;
	private final WritableByteChannel out;

	public DecryptWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> queue, boolean shouldAuthenticate, WritableByteChannel out) {
		super(lock, blockDone, currentBlock, queue);
		this.shouldAuthenticate = shouldAuthenticate;
		this.out = out;
	}

	@Override
	protected ByteBuffer process(BlocksData data) throws CryptingException {
		final Cipher cipher = initCipher(data.startBlockNum);
		final Mac mac = initMac();

		final ByteBuffer plaintextBuf = ByteBuffer.allocate(cipher.getOutputSize(CONTENT_MAC_BLOCK) * data.numBlocks);

		final ByteBuffer ciphertextBuf = data.buffer.asReadOnlyBuffer();
		final ByteBuffer macBuf = data.buffer.asReadOnlyBuffer();

		for (long blockNum = data.startBlockNum; blockNum < data.startBlockNum + data.numBlocks; blockNum++) {
			assert (blockNum - data.startBlockNum) < BlocksData.MAX_NUM_BLOCKS;
			assert (blockNum - data.startBlockNum) * CONTENT_MAC_BLOCK < Integer.MAX_VALUE;
			final int pos = (int) (blockNum - data.startBlockNum) * (CONTENT_MAC_BLOCK + mac.getMacLength());
			ciphertextBuf.limit(Math.min(data.buffer.limit() - mac.getMacLength(), pos + CONTENT_MAC_BLOCK));
			ciphertextBuf.position(pos);
			try {
				macBuf.limit(ciphertextBuf.limit() + mac.getMacLength());
				macBuf.position(ciphertextBuf.limit());
			} catch (IllegalArgumentException e) {
				throw new DecryptFailedException("Invalid file content, missing MAC.");
			}
			if (shouldAuthenticate) {
				checkMac(mac, blockNum, ciphertextBuf, macBuf);
			}
			ciphertextBuf.position(pos);
			decrypt(cipher, ciphertextBuf, plaintextBuf);
		}

		plaintextBuf.flip();
		return plaintextBuf;
	}

	@Override
	protected void write(ByteBuffer processedBytes) throws IOException {
		out.write(processedBytes);
	}

	protected abstract Cipher initCipher(long startBlockNum);

	protected abstract Mac initMac();

	protected abstract void checkMac(Mac mac, long blockNum, ByteBuffer ciphertextBuf, ByteBuffer macBuf) throws MacAuthenticationFailedException;

	protected abstract void decrypt(Cipher cipher, ByteBuffer ciphertextBuf, ByteBuffer plaintextBuf) throws DecryptFailedException;

}
