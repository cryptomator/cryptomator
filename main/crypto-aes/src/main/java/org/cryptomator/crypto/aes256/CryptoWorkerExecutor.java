package org.cryptomator.crypto.aes256;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CryptoWorkerExecutor {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoWorkerExecutor.class);

	private final int numWorkers;
	private final Lock lock;
	private final Condition blockDone;
	private final AtomicLong currentBlock;
	private final BlockingQueue<BlocksData> inputQueue;
	private final ExecutorService executorService;
	private final CompletionService<Void> completionService;
	private volatile boolean acceptWork;

	/**
	 * Starts as many {@link CryptoWorker} as specified in the constructor, that start working immediately on the items submitted via {@link #offer(BlocksData, long, TimeUnit)}.
	 */
	public CryptoWorkerExecutor(int numWorkers, WorkerFactory workerFactory) {
		this.numWorkers = numWorkers;
		this.lock = new ReentrantLock();
		this.blockDone = lock.newCondition();
		this.currentBlock = new AtomicLong();
		this.inputQueue = new LinkedBlockingQueue<>(numWorkers * 2); // one cycle read-ahead
		this.executorService = Executors.newFixedThreadPool(numWorkers);
		this.completionService = new ExecutorCompletionService<>(executorService);
		this.acceptWork = true;

		// start workers:
		for (int i = 0; i < numWorkers; i++) {
			final CryptoWorker worker = workerFactory.createWorker(lock, blockDone, currentBlock, inputQueue);
			completionService.submit(worker);
		}
	}

	/**
	 * Adds work to the work queue. On timeout all workers will be shut down.
	 * 
	 * @see BlockingQueue#offer(Object, long, TimeUnit)
	 * @return <code>true</code> if the work has been added in time. <code>false</code> in any other case.
	 */
	public boolean offer(BlocksData data, long timeout, TimeUnit unit) {
		if (!acceptWork) {
			return false;
		}
		try {
			final boolean success = inputQueue.offer(data, timeout, unit);
			if (!success) {
				this.acceptWork = false;
				inputQueue.clear();
				poisonWorkers(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
			return success;
		} catch (InterruptedException e) {
			LOG.error("Interrupted thread.", e);
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
		return false;
	}

	/**
	 * Graceful shutdown of this executor, waiting for all jobs to finish (normally or by throwing exceptions).
	 * 
	 * @param timeout Maximum time spent <em>per worker</em> to wait for a graceful shutdown (technically worst case is: <code>2 * numWorkers * time</code>)
	 * @param unit Timeout unit
	 * @throws ExecutionException If any of the workers failed.
	 */
	public void waitUntilDone(long timeout, TimeUnit unit) throws ExecutionException {
		this.acceptWork = false;
		try {
			// fail fast, if workers are done before being poisoned (i.e. exceptionally):
			for (Future<Void> task = completionService.poll(); task != null; task = completionService.poll()) {
				task.get(); // this will most likely throw an ExecutionException
			}
			// if we got to this point without any exception, all workers are still running, so lets poison them:
			poisonWorkers(timeout, unit);
			// now workers will one after another finish their work, potentially throwing an ExecutionException:
			for (Future<Void> task = completionService.poll(timeout, unit); task != null; task = completionService.poll(timeout, unit)) {
				task.get();
			}
		} catch (InterruptedException e) {
			LOG.error("Interrupted thread.", e);
			Thread.currentThread().interrupt();
		} finally {
			// shutdown either after normal decryption or if ANY worker threw an exception:
			executorService.shutdownNow();
		}
	}

	private void poisonWorkers(long timeout, TimeUnit unit) throws InterruptedException {
		// add enough poison for each worker:
		for (int i = 0; i < numWorkers; i++) {
			if (!inputQueue.offer(CryptoWorker.POISON, timeout, unit)) {
				break;
			}
		}
	}

	@FunctionalInterface
	interface WorkerFactory {
		CryptoWorker createWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> inputQueue);
	}

}
