/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import com.google.common.util.concurrent.Runnables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

@Singleton
public class ShutdownHook extends Thread {

	private static final int PRIO_VERY_LAST = Integer.MIN_VALUE;
	public static final int PRIO_LAST = PRIO_VERY_LAST + 1;
	public static final int PRIO_DEFAULT = 0;
	public static final int PRIO_FIRST = Integer.MAX_VALUE;
	private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
	private static final OrderedTask POISON = new OrderedTask(PRIO_VERY_LAST, Runnables.doNothing());
	private final Queue<OrderedTask> tasks = new PriorityBlockingQueue<>();

	@Inject
	ShutdownHook() {
		super(null, null, "ShutdownTasks", 0);
		Runtime.getRuntime().addShutdownHook(this);
		LOG.debug("Registered shutdown hook.");
	}

	@Override
	public void run() {
		LOG.debug("Running graceful shutdown tasks...");
		tasks.add(POISON);
		Runnable task;
		while ((task = tasks.remove()) != POISON) {
			try {
				task.run();
			} catch (RuntimeException e) {
				LOG.error("Exception while shutting down.", e);
			}
		}
	}

	/**
	 * Schedules a task to be run during shutdown with default order
	 *
	 * @param task The task to be scheduled
	 */
	public void runOnShutdown(Runnable task) {
		runOnShutdown(PRIO_DEFAULT, task);
	}

	/**
	 * Schedules a task to be run with the given priority
	 *
	 * @param priority Tasks with high priority will be run before task with lower priority
	 * @param task The task to be scheduled
	 */
	public void runOnShutdown(int priority, Runnable task) {
		tasks.add(new OrderedTask(priority, task));
	}

	private static class OrderedTask implements Comparable<OrderedTask>, Runnable {

		private final int priority;
		private final Runnable task;

		public OrderedTask(int priority, Runnable task) {
			this.priority = priority;
			this.task = task;
		}

		@Override
		public int compareTo(OrderedTask other) {
			// overflow-safe signum impl:
			if (this.priority > other.priority) {
				return -1; // higher prio -> this before other
			} else if (this.priority < other.priority) {
				return +1; // lower prio -> other before this
			} else {
				return 0; // same prio
			}
		}

		@Override
		public void run() {
			task.run();
		}
	}

}