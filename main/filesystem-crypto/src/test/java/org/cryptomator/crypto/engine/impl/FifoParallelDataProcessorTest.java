package org.cryptomator.crypto.engine.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class FifoParallelDataProcessorTest {

	@Test
	public void testStrictFifoOrder() throws InterruptedException {
		FifoParallelDataProcessor<Integer> processor = new FifoParallelDataProcessor<>(4, 10);
		processor.submit(new IntegerJob(100, 1));
		processor.submit(new IntegerJob(50, 2));
		processor.submitPreprocessed(3);
		processor.submit(new IntegerJob(10, 4));
		processor.submit(new IntegerJob(10, 5));
		processor.submitPreprocessed(6);

		Assert.assertEquals(1, (int) processor.processedData());
		Assert.assertEquals(2, (int) processor.processedData());
		Assert.assertEquals(3, (int) processor.processedData());
		Assert.assertEquals(4, (int) processor.processedData());
		Assert.assertEquals(5, (int) processor.processedData());
		Assert.assertEquals(6, (int) processor.processedData());
	}

	@Test
	public void testBlockingBehaviour() throws InterruptedException {
		FifoParallelDataProcessor<Integer> processor = new FifoParallelDataProcessor<>(1, 1);
		processor.submit(new IntegerJob(100, 1)); // runs immediatley
		processor.submit(new IntegerJob(100, 2)); // #1 in queue

		Thread t1 = new Thread(() -> {
			try {
				processor.submit(new IntegerJob(10, 3)); // #2 in queue
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		t1.start();
		t1.join(10);
		// job 3 should not have been submitted by now, thus t1 is still alive
		Assert.assertTrue(t1.isAlive());
		Assert.assertEquals(1, (int) processor.processedData());
		Assert.assertEquals(2, (int) processor.processedData());
		Assert.assertEquals(3, (int) processor.processedData());
		Assert.assertFalse(t1.isAlive());
	}

	@Test
	public void testInterruptionDuringSubmission() throws InterruptedException {
		FifoParallelDataProcessor<Integer> processor = new FifoParallelDataProcessor<>(1, 1);
		processor.submit(new IntegerJob(100, 1)); // runs immediatley
		processor.submit(new IntegerJob(100, 2)); // #1 in queue

		final AtomicBoolean interruptedExceptionThrown = new AtomicBoolean(false);
		Thread t1 = new Thread(() -> {
			try {
				processor.submit(new IntegerJob(10, 3)); // #2 in queue
			} catch (InterruptedException e) {
				interruptedExceptionThrown.set(true);
				Thread.currentThread().interrupt();
			}
		});
		t1.start();
		t1.join(10);
		t1.interrupt();
		t1.join(10);
		// job 3 should not have been submitted by now, thus t1 is still alive
		Assert.assertFalse(t1.isAlive());
		Assert.assertTrue(interruptedExceptionThrown.get());
		Assert.assertEquals(1, (int) processor.processedData());
		Assert.assertEquals(2, (int) processor.processedData());
	}

	private static class IntegerJob implements Callable<Integer> {

		private final long waitMillis;
		private final int result;

		public IntegerJob(long waitMillis, int result) {
			this.waitMillis = waitMillis;
			this.result = result;
		}

		@Override
		public Integer call() throws Exception {
			Thread.sleep(waitMillis);
			return result;
		}

	}

}
