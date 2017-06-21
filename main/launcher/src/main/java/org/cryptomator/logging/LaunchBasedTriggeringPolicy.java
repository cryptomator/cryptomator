/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.logging;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;

/**
 * Triggers a roll-over on the first log event, so each launched application instance will rotate the log.
 */
public class LaunchBasedTriggeringPolicy<E> extends TriggeringPolicyBase<E> {

	private final AtomicBoolean shouldTrigger = new AtomicBoolean(true);

	@Override
	public boolean isTriggeringEvent(File activeFile, E event) {
		return shouldTrigger.get() && shouldTrigger.getAndSet(false);
	}

}
