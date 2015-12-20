package org.cryptomator.crypto.engine.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes long-running computations and returns the result strictly in order of the job submissions, no matter how long each job takes.
 * 
 * The internally used thread pool is shut down automatically as soon as this FifiParallelDataProcessor is no longer referenced (see Finalization behaviour of {@link ThreadPoolExecutor}).
 */
class FifoParallelDataProcessor<T> {

	private final BlockingQueue<SequencedFutureResult> processedData = new PriorityBlockingQueue<>();
	private final AtomicLong jobSequence = new AtomicLong();
	private final BlockingQueue<Runnable> workQueue;
	private final ExecutorService executorService;

	/**
	 * @param numThreads How many jobs can run in parallel.
	 * @param workQueueSize Maximum number of jobs accepted without blocking, when no results are polled from {@link #processedData()}.
	 */
	public FifoParallelDataProcessor(int numThreads, int workQueueSize) {
		this.workQueue = new ArrayBlockingQueue<>(workQueueSize);
		this.executorService = new ThreadPoolExecutor(numThreads, numThreads, 1, TimeUnit.SECONDS, workQueue, this::rejectedExecution);
	}

	/**
	 * Enqueues tasks into the blocking queue, if they can not be executed immediately.
	 * 
	 * @see ThreadPoolExecutor#execute(Runnable)
	 */
	private void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		try {
			this.workQueue.put(r);
		} catch (InterruptedException e) {
			throw new SneakyInterruptedException(e);
		}
	}

	/**
	 * Enqueues a job for execution. The results of multiple submissions can be polled in FIFO order using {@link #processedData()}.
	 * 
	 * @param processingJob A task, that will compute a result.
	 * @throws InterruptedException
	 */
	void submit(Callable<T> processingJob) throws InterruptedException {
		try {
			Future<T> future = executorService.submit(processingJob);
			processedData.offer(new SequencedFutureResult(future, jobSequence.getAndIncrement()));
		} catch (SneakyInterruptedException e) {
			throw e.getCause();
		}
	}

	/**
	 * Submits already pre-processed data, that can be polled in FIFO order from {@link #processedData()}.
	 * 
	 * @throws InterruptedException
	 */
	void submitPreprocessed(T preprocessedData) throws InterruptedException {
		this.submit(() -> {
			return preprocessedData;
		});
	}

	/**
	 * Result of previously {@link #submit(Callable) submitted} jobs in the same order as they have been submitted. Blocks if the job didn't finish yet.
	 * 
	 * @return Next job result
	 * @throws InterruptedException If the calling thread was interrupted while waiting for the next result.
	 */
	T processedData() throws InterruptedException {
		return processedData.take().get();
	}

	private class SequencedFutureResult implements Comparable<SequencedFutureResult> {

		private final Future<T> result;
		private final long sequenceNumber;

		public SequencedFutureResult(Future<T> result, long sequenceNumber) {
			this.result = result;
			this.sequenceNumber = sequenceNumber;
		}

		public T get() throws InterruptedException {
			try {
				return result.get();
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RuntimeException) {
					throw (RuntimeException) e.getCause();
				} else {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public int compareTo(SequencedFutureResult other) {
			return Long.compare(this.sequenceNumber, other.sequenceNumber);
		}

	}

	private static class SneakyInterruptedException extends RuntimeException {

		private static final long serialVersionUID = 331817765088138556L;

		public SneakyInterruptedException(InterruptedException cause) {
			super(cause);
		}

		@Override
		public InterruptedException getCause() {
			return (InterruptedException) super.getCause();
		}

	}

}
