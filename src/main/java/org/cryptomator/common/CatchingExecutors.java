package org.cryptomator.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//Inspired by: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html#afterExecute(java.lang.Runnable,java.lang.Throwable)
public final class CatchingExecutors {

	private static final Logger LOG = LoggerFactory.getLogger(CatchingExecutors.class);

	private CatchingExecutors() { /* NO-OP */ }

	public static class CatchingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

		public CatchingScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
			super(corePoolSize, threadFactory);
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
			Runnable oneShot = () -> this.execute(command);
			return super.scheduleAtFixedRate(oneShot, initialDelay, period, unit);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
			Runnable oneShot = () -> this.execute(command);
			return super.scheduleWithFixedDelay(oneShot, initialDelay, delay, unit);
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			super.afterExecute(runnable, throwable);
			afterExecuteInternal(runnable, throwable);
		}
	}

	public static class CatchingThreadPoolExecutor extends ThreadPoolExecutor {

		public CatchingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			super.afterExecute(runnable, throwable);
			afterExecuteInternal(runnable, throwable);
		}
	}

	private static void afterExecuteInternal(Runnable runnable, Throwable throwable) {
		if (throwable != null) {
			callHandler(Thread.currentThread(), throwable);
		} else if (runnable instanceof Task<?> t) {
			afterExecuteTask(t);
		} else if (runnable instanceof Future<?> f) {
			afterExecuteFuture(f);
		}
		//Errors in this method are delegated to the UncaughtExceptionHandler of the current thread
	}

	private static void callHandler(Thread thread, Throwable throwable) {
		Objects.requireNonNullElseGet(thread.getUncaughtExceptionHandler(), CatchingExecutors::fallbackHandler).uncaughtException(thread, throwable);
	}

	private static Thread.UncaughtExceptionHandler fallbackHandler() {
		return (thread, throwable) -> LOG.error("FALLBACK: Uncaught exception in " + thread.getName(), throwable);
	}

	private static void afterExecuteTask(Task<?> task) {
		var caller = Thread.currentThread();
		Platform.runLater(() -> {
			if (task.getOnFailed() == null) {
				callHandler(caller, task.getException());
			}
		});
	}

	private static void afterExecuteFuture(Future<?> future) {
		if (future instanceof ScheduledFuture<?> && !future.isDone()) {
			//we assume that this must be a repeated ScheduledFutureTask, where the done-status is only set when not executed anymore
			//see also https://github.com/cryptomator/cryptomator/pull/2422
			return;
		}

		try {
			future.get();
		} catch (CancellationException ce) {
			//Ignore
		} catch (ExecutionException ee) {
			callHandler(Thread.currentThread(), ee.getCause());
		} catch (InterruptedException ie) {
			//Ignore/Reset
			Thread.currentThread().interrupt();
		}
	}
}