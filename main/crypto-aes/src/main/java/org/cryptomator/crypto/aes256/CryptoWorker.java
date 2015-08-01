package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.cryptomator.crypto.exceptions.CryptingException;

abstract class CryptoWorker implements Callable<Void> {

	static final BlocksData POISON = new BlocksData(ByteBuffer.allocate(0), -1L, 0);

	final Lock lock;
	final Condition blockDone;
	final AtomicLong currentBlock;
	final BlockingQueue<BlocksData> queue;

	public CryptoWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> queue) {
		this.lock = lock;
		this.blockDone = blockDone;
		this.currentBlock = currentBlock;
		this.queue = queue;
	}

	@Override
	public final Void call() throws IOException {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				final BlocksData blocksData = queue.take();
				if (blocksData == POISON) {
					// put poison back in for other threads:
					break;
				}
				final ByteBuffer processedBytes = this.process(blocksData);
				lock.lock();
				try {
					while (currentBlock.get() != blocksData.startBlockNum) {
						blockDone.await();
					}
					assert currentBlock.get() == blocksData.startBlockNum;
					// yay, its my turn!
					this.write(processedBytes);
					// signal worker working on next block:
					currentBlock.set(blocksData.startBlockNum + blocksData.numBlocks);
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

	protected abstract ByteBuffer process(BlocksData block) throws CryptingException;

	protected abstract void write(ByteBuffer processedBytes) throws IOException;

}
