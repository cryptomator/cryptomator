/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executes long-running computations and returns the result strictly in order of the job submissions, no matter how long each job takes.
 * 
 * The internally used thread pool is shut down automatically as soon as this FifiParallelDataProcessor is no longer referenced (see Finalization behaviour of {@link ThreadPoolExecutor}).
 */
class FifoParallelDataProcessor<T> {

	private final BlockingQueue<Future<T>> processedData;
	private final ExecutorService executorService;

	/**
	 * @param numThreads How many jobs can run in parallel.
	 * @param workAhead Maximum number of jobs accepted in {@link #submit(Callable)} without blocking until results are polled from {@link #processedData()}.
	 */
	public FifoParallelDataProcessor(int workAhead, ExecutorService executorService) {
		this.processedData = new ArrayBlockingQueue<>(workAhead);
		this.executorService = executorService;
	}

	/**
	 * Enqueues a job for execution. The results of multiple submissions can be polled in FIFO order using {@link #processedData()}.
	 * 
	 * @param processingJob A task, that will compute a result.
	 * @throws InterruptedException
	 */
	void submit(Callable<T> processingJob) throws InterruptedException {
		Future<T> future = executorService.submit(processingJob);
		processedData.put(future);
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
	T processedData() throws InterruptedException, ExecutionException {
		return processedData.take().get();
	}

}
