package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.cryptomator.crypto.exceptions.CryptingException;

abstract class EncryptWorker extends CryptoWorker implements AesCryptographicConfiguration {

	private final WritableByteChannel out;

	public EncryptWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<Block> queue, WritableByteChannel out) {
		super(lock, blockDone, currentBlock, queue);
		this.out = out;
	}

	@Override
	protected ByteBuffer process(Block block) throws CryptingException {
		final ByteBuffer buf = ByteBuffer.allocateDirect(block.buffer.limit() + 32);
		encrypt(block, buf);
		final ByteBuffer ciphertextBuffer = buf.duplicate();
		ciphertextBuffer.flip();
		final byte[] mac = mac(block.blockNumber, ciphertextBuffer);
		buf.put(mac);
		return buf;
	}

	@Override
	protected void write(ByteBuffer processedBytes) throws IOException {
		processedBytes.flip();
		out.write(processedBytes);
	}

	protected abstract byte[] mac(long blockNumber, ByteBuffer ciphertext);

	protected abstract void encrypt(Block block, ByteBuffer ciphertext);

}
