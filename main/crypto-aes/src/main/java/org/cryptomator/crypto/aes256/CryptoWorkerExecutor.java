package org.cryptomator.crypto.aes256;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
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
	private final Future<Void> allWork;

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

		// start workers:
		final CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
		final Collection<Future<?>> workers = new ArrayList<>(numWorkers);
		for (int i = 0; i < numWorkers; i++) {
			final CryptoWorker worker = workerFactory.createWorker(lock, blockDone, currentBlock, inputQueue);
			workers.add(completionService.submit(worker));
		}
		final Supervisor supervisor = new Supervisor(workers, completionService);
		this.allWork = executorService.submit(supervisor);
	}

	/**
	 * Adds work to the work queue. On timeout all workers will be shut down.
	 * 
	 * @see BlockingQueue#offer(Object, long, TimeUnit)
	 * @return <code>true</code> if the work has been added in time. <code>false</code> in any other case.
	 */
	public boolean offer(BlocksData data, long timeout, TimeUnit unit) {
		if (allWork.isDone()) {
			return false;
		}
		try {
			final boolean success = inputQueue.offer(data, timeout, unit);
			if (!success) {
				LOG.error("inputQueue is full.");
				inputQueue.clear();
				allWork.cancel(true);
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
	 * @param timeout Maximum time spent <em>per worker</em> to wait for a graceful shutdown
	 * @param unit Timeout unit
	 * @throws ExecutionException If any of the workers failed.
	 */
	public void waitUntilDone(long timeout, TimeUnit unit) throws ExecutionException {
		try {
			if (allWork.isDone()) {
				// Work is done before workers being poisoned? This will most likely throw an ExecutionException:
				allWork.get();
			} else if (!poisonWorkers(timeout, unit)) {
				// Attempt to enqueue poison pill for all workers failed:
				allWork.cancel(true);
			} else {
				// All poisons enqueued successfully. Now wait for termination by poison or exception:
				allWork.get();
			}
		} catch (InterruptedException e) {
			LOG.error("Interrupted thread.", e);
			Thread.currentThread().interrupt();
		} catch (CancellationException e) {
			throw new ExecutionException("Work canceled", e);
		} finally {
			// in any case (normal or exceptional execution): shutdown executor including all workers and supervisor:
			executorService.shutdownNow();
		}
	}

	private boolean poisonWorkers(long timeout, TimeUnit unit) throws InterruptedException {
		// add enough poison for each worker; each worker will consume excatly one:
		for (int i = 0; i < numWorkers; i++) {
			if (!inputQueue.offer(CryptoWorker.POISON, timeout, unit)) {
				return false;
			}
		}
		return true;
	}

	@FunctionalInterface
	interface WorkerFactory {
		CryptoWorker createWorker(Lock lock, Condition blockDone, AtomicLong currentBlock, BlockingQueue<BlocksData> inputQueue);
	}

	/**
	 * A supervisor watches the work results of a collection of workers. The supervisor waits for all workers to finish.
	 * The supvervisor itself does not cause any exceptions, but if <em>one</em> worker fails, all other workers are cancelled immediately and the exception propagates through this supvervisor.
	 * Anyone waiting for the supervisor to finish will thus effectively wait for all supvervisees to finish.
	 */
	private static class Supervisor implements Callable<Void> {

		private final Collection<Future<?>> workers;
		private final CompletionService<?> completionService;

		public Supervisor(Collection<Future<?>> workers, CompletionService<?> completionService) {
			this.workers = workers;
			this.completionService = completionService;
		}

		@Override
		public Void call() throws ExecutionException {
			try {
				for (int i = 0; i < workers.size(); i++) {
					try {
						// any ExecutionException thrown here will propagate up (after work is canceled in finally block)
						completionService.take().get();
					} catch (CancellationException ignore) {
					}
				}
			} catch (InterruptedException e) {
				// supervisor may be interrupted when executorservice is shut down.
				Thread.currentThread().interrupt();
			} finally {
				// make sure, that at the end of the day all remaining workers leave the building.
				for (Future<?> worker : workers) {
					worker.cancel(true);
				}
			}
			// no exception up to this point -> all workers finished work normally.
			return null;
		}
	}

}
