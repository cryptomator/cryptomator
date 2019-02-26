/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import javafx.beans.value.ObservableValue;
import org.cryptomator.common.settings.Settings;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class DebugMode {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DebugMode.class);

	private final Settings settings;
	private final LoggerContext context;

	@Inject
	public DebugMode(Settings settings, LoggerContext context) {
		this.settings = settings;
		this.context = context;
	}

	public void initialize() {
		setLogLevels(settings.debugMode().get());
		settings.debugMode().addListener(this::logLevelChanged);
	}

	private void logLevelChanged(@SuppressWarnings("unused") ObservableValue<? extends Boolean> observable, @SuppressWarnings("unused") Boolean oldValue, Boolean newValue) {
		setLogLevels(newValue);
	}

	private void setLogLevels(boolean debugMode) {
		if (debugMode) {
			setLogLevels(LoggerModule.DEBUG_LOG_LEVELS);
			LOG.debug("Debug mode enabled");
		} else {
			LOG.debug("Debug mode disabled");
			setLogLevels(LoggerModule.DEFAULT_LOG_LEVELS);
		}
	}

	private void setLogLevels(Map<String, Level> logLevels) {
		for (Map.Entry<String, Level> loglevel : logLevels.entrySet()) {
			Logger logger = context.getLogger(loglevel.getKey());
			logger.setLevel(loglevel.getValue());
		}
	}

}