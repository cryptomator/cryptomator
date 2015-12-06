package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.cryptomator.crypto.exceptions.CryptingException;

abstract class CryptoWorker implements Callable<Void> {

	static final BlocksData POISON = new BlocksData(ByteBuffer.allocate(0), -1L, 0);

	private final Lock lock;
	private final Condition blockDone;
	private final AtomicLong currentBlock;
	private final BlockingQueue<BlocksData> queue;

	public CryptoWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> queue) {
		this.lock = lock;
		this.blockDone = blockDone;
		this.currentBlock = currentBlock;
		this.queue = queue;
	}

	@Override
	public final Void call() throws IOException, TimeoutException {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				final BlocksData blocksData = queue.take();
				if (blocksData == POISON) {
					break;
				}
				final ByteBuffer processedBytes = this.process(blocksData);
				lock.lock();
				try {
					while (currentBlock.get() != blocksData.startBlockNum) {
						if (!blockDone.await(1, TimeUnit.SECONDS)) {
							throw new TimeoutException("Waited too long to write block " + blocksData.startBlockNum + "; Current block " + currentBlock.get());
						}
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
			// will happen for executorService.shutdownNow() or future.cancel()
			Thread.currentThread().interrupt();
		}
		return null;
	}

	protected abstract ByteBuffer process(BlocksData block) throws CryptingException;

	protected abstract void write(ByteBuffer processedBytes) throws IOException;

}
