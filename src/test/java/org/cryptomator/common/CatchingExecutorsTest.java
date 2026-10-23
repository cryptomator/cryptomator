package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

class CatchingExecutorsTest {

	private final CompletableFuture<Throwable> uncaught = new CompletableFuture<>();
	private final ThreadFactory threadFactory = r -> {
		var thread = new Thread(r);
		thread.setUncaughtExceptionHandler((_, e) -> uncaught.complete(e));
		return thread;
	};
	private final CatchingExecutors.CatchingThreadPoolExecutor executor = new CatchingExecutors.CatchingThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);

	@Test
	public void testCompletableFutureTaskReleasesThread() throws Exception {
		CompletableFuture.runAsync(() -> {}, executor).get(1, TimeUnit.SECONDS);
		executor.shutdown();

		Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
	}

	@Test
	public void testSubmittedTaskFailureIsReported() throws Exception {
		executor.submit(() -> {
			throw new IllegalStateException();
		});

		Assertions.assertInstanceOf(IllegalStateException.class, uncaught.get(1, TimeUnit.SECONDS));
		executor.shutdown();
	}

	@Test
	public void testPeriodicTaskKeepsRunning() throws Exception {
		var scheduler = new CatchingExecutors.CatchingScheduledThreadPoolExecutor(1, threadFactory);
		var runs = new CountDownLatch(3);

		scheduler.scheduleAtFixedRate(runs::countDown, 0, 10, TimeUnit.MILLISECONDS);

		try {
			Assertions.assertTrue(runs.await(1, TimeUnit.SECONDS));
		} finally {
			scheduler.shutdownNow();
		}
	}

}
