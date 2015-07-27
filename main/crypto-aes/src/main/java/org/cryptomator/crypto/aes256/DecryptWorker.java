package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.cryptomator.crypto.exceptions.CryptingException;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;

abstract class DecryptWorker extends CryptoWorker implements AesCryptographicConfiguration {

	private final boolean shouldAuthenticate;
	private final WritableByteChannel out;

	public DecryptWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<Block> queue, boolean shouldAuthenticate, WritableByteChannel out) {
		super(lock, blockDone, currentBlock, queue);
		this.shouldAuthenticate = shouldAuthenticate;
		this.out = out;
	}

	@Override
	protected ByteBuffer process(Block block) throws CryptingException {
		if (block.buffer.length < 32) {
			throw new DecryptFailedException("Invalid file content, missing MAC.");
		}

		// check MAC of current block:
		if (shouldAuthenticate) {
			checkMac(block);
		}

		// decrypt block:
		return decrypt(block);
	}

	@Override
	protected void write(ByteBuffer processedBytes) throws IOException {
		processedBytes.flip();
		out.write(processedBytes);
	}

	protected abstract void checkMac(Block block) throws MacAuthenticationFailedException;

	protected abstract ByteBuffer decrypt(Block block);

}
