package org.cryptomator.crypto.engine.impl;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

abstract class AbstractFileContentProcessor implements Closeable {

	private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 0;

	private final BlockingQueue<BytesWithSequenceNumber> processedData = new PriorityBlockingQueue<>();
	private final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(NUM_WORKERS + READ_AHEAD);
	private final ExecutorService executorService = new ThreadPoolExecutor(1, NUM_WORKERS, 1, TimeUnit.SECONDS, workQueue);
	private final AtomicLong jobSequence = new AtomicLong();

	/**
	 * Enqueues a job for execution. The results of multiple submissions can be polled in FIFO order using {@link #processedData()}.
	 * 
	 * @param processingJob A ByteBuffer-generating task.
	 */
	protected void submit(Callable<ByteBuffer> processingJob) {
		Future<ByteBuffer> result = executorService.submit(processingJob);
		processedData.offer(new BytesWithSequenceNumber(result, jobSequence.getAndIncrement()));
	}

	/**
	 * Submits already processed data, that can be polled in FIFO order from {@link #processedData()}.
	 */
	protected void submitPreprocessed(ByteBuffer preprocessedData) {
		Future<ByteBuffer> resolvedFuture = ConcurrentUtils.constantFuture(preprocessedData);
		processedData.offer(new BytesWithSequenceNumber(resolvedFuture, jobSequence.getAndIncrement()));
	}

	/**
	 * Result of previously {@link #submit(Callable) submitted} jobs in the same order as they have been submitted. Blocks if the job didn't finish yet.
	 * 
	 * @return Next job result
	 * @throws InterruptedException If the calling thread was interrupted while waiting for the next result.
	 */
	protected ByteBuffer processedData() throws InterruptedException {
		return processedData.take().get();
	}

	@Override
	public void close() {
		executorService.shutdown();
	}

}
