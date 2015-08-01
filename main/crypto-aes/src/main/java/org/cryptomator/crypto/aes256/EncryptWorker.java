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
import org.cryptomator.crypto.exceptions.EncryptFailedException;

abstract class EncryptWorker extends CryptoWorker implements AesCryptographicConfiguration {

	private final WritableByteChannel out;

	public EncryptWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> queue, WritableByteChannel out) {
		super(lock, blockDone, currentBlock, queue);
		this.out = out;
	}

	@Override
	protected ByteBuffer process(BlocksData data) throws CryptingException {
		final Cipher cipher = initCipher(data.startBlockNum);
		final Mac mac = initMac();

		final ByteBuffer ciphertextBuf = ByteBuffer.allocate((cipher.getOutputSize(CONTENT_MAC_BLOCK) + mac.getMacLength()) * data.numBlocks);
		final ByteBuffer plaintextBuf = data.buffer.asReadOnlyBuffer();

		for (long blockNum = data.startBlockNum; blockNum < data.startBlockNum + data.numBlocks; blockNum++) {
			final int pos = (int) (blockNum - data.startBlockNum) * CONTENT_MAC_BLOCK;
			plaintextBuf.limit(Math.min(data.buffer.limit(), pos + CONTENT_MAC_BLOCK));
			encrypt(cipher, plaintextBuf, ciphertextBuf);
			final ByteBuffer toMac = ciphertextBuf.asReadOnlyBuffer();
			toMac.limit(ciphertextBuf.position());
			toMac.position((int) (blockNum - data.startBlockNum) * (CONTENT_MAC_BLOCK + mac.getMacLength()));
			ciphertextBuf.put(calcMac(mac, blockNum, toMac));
		}

		ciphertextBuf.flip();
		return ciphertextBuf;
	}

	@Override
	protected void write(ByteBuffer processedBytes) throws IOException {
		out.write(processedBytes);
	}

	protected abstract Cipher initCipher(long startBlockNum);

	protected abstract Mac initMac();

	protected abstract byte[] calcMac(Mac mac, long blockNum, ByteBuffer ciphertextBuf);

	protected abstract void encrypt(Cipher cipher, ByteBuffer plaintextBuf, ByteBuffer ciphertextBuf) throws EncryptFailedException;

}
