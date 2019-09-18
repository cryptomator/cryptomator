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

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class CleanShutdownPerformer extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(CleanShutdownPerformer.class);
	private final ConcurrentMap<Runnable, Boolean> tasks = new ConcurrentHashMap<>();

	@Inject
	CleanShutdownPerformer() {
		super(null, null, "ShutdownTasks", 0);
	}

	@Override
	public void run() {
		LOG.debug("Running graceful shutdown tasks...");
		tasks.keySet().forEach(r -> {
			try {
				r.run();
			} catch (RuntimeException e) {
				LOG.error("Exception while shutting down.", e);
			}
		});
		tasks.clear();
	}

	void scheduleShutdownTask(Runnable task) {
		tasks.put(task, Boolean.TRUE);
	}

	void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(this);
	}
}