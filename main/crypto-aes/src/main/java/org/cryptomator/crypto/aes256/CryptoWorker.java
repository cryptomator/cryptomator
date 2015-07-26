package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.cryptomator.crypto.exceptions.CryptingException;

abstract class CryptoWorker implements Callable<Void> {

	static final Block POISON = new Block(0, new byte[0], -1L);

	final Lock lock;
	final Condition blockDone;
	final AtomicLong currentBlock;
	final BlockingQueue<Block> queue;

	public CryptoWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<Block> queue) {
		this.lock = lock;
		this.blockDone = blockDone;
		this.currentBlock = currentBlock;
		this.queue = queue;
	}

	@Override
	public final Void call() throws IOException {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				final Block block = queue.take();
				if (block == POISON) {
					// put poison back in for other threads:
					break;
				}
				final byte[] processedBytes = this.process(block);
				lock.lock();
				try {
					while (currentBlock.get() != block.blockNumber) {
						blockDone.await();
					}
					assert currentBlock.get() == block.blockNumber;
					// yay, its my turn!
					this.write(processedBytes);
					// signal worker working on next block:
					currentBlock.set(block.blockNumber + 1);
					blockDone.signalAll();
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	protected abstract byte[] process(Block block) throws CryptingException;

	protected abstract void write(byte[] processedBytes) throws IOException;

}
