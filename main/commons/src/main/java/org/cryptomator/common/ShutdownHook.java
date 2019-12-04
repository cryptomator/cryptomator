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
import java.util.concurrent.ConcurrentLinkedQueue;

@Singleton
public class ShutdownHook extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
	private static final Runnable POISON = Runnables.doNothing();
	
	private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

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

	public void runOnShutdown(Runnable task) {
		tasks.add(task);
	}
	
}