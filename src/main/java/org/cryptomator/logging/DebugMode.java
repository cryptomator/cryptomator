/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.logging;

import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;

@Singleton
public class DebugMode {

	private static final Logger LOG = LoggerFactory.getLogger(DebugMode.class);

	private final Settings settings;

	@Inject
	public DebugMode(Settings settings) {
		this.settings = settings;
	}

	public void initialize() {
		setLogLevels(settings.debugMode.get());
		settings.debugMode.addListener(this::logLevelChanged);
	}

	private void logLevelChanged(@SuppressWarnings("unused") ObservableValue<? extends Boolean> observable, @SuppressWarnings("unused") Boolean oldValue, Boolean newValue) {
		setLogLevels(newValue);
	}

	private void setLogLevels(boolean debugMode) {
		var configurator = LogbackConfiguratorFactory.provider();
		if (debugMode) {
			configurator.setLogLevels(LogbackConfigurator.DEBUG_LOG_LEVELS);
			LOG.debug("Debug mode enabled");
		} else {
			LOG.debug("Debug mode disabled");
			configurator.setLogLevels(LogbackConfigurator.DEFAULT_LOG_LEVELS);
		}
	}

}