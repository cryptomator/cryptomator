/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CleanShutdownPerformer extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(CleanShutdownPerformer.class);
	static final ConcurrentMap<Runnable, Boolean> SHUTDOWN_TASKS = new ConcurrentHashMap<>();

	@Override
	public void run() {
		LOG.debug("Running graceful shutdown tasks...");
		SHUTDOWN_TASKS.keySet().forEach(r -> {
			try {
				r.run();
			} catch (RuntimeException e) {
				LOG.error("Exception while shutting down.", e);
			}
		});
		SHUTDOWN_TASKS.clear();
		LOG.info("Goodbye.");
	}

	static void scheduleShutdownTask(Runnable task) {
		SHUTDOWN_TASKS.put(task, Boolean.TRUE);
	}

	static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new CleanShutdownPerformer());
	}
}