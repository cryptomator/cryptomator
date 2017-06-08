/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.logging;

import static java.util.Arrays.asList;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.common.settings.Settings;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Singleton
public class DebugMode {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DebugMode.class);

	private static final Collection<LoggerUpgrade> LOGGER_UPGRADES = asList( //
			loggerUpgrade(org.slf4j.Logger.ROOT_LOGGER_NAME, Level.INFO), //
			loggerUpgrade("org.cryptomator", Level.TRACE), //
			loggerUpgrade("org.eclipse.jetty.server.HttpChannel", Level.DEBUG) //
	);

	private final Settings settings;

	@Inject
	public DebugMode(Settings settings) {
		this.settings = settings;
	}

	public void initialize() {
		if (settings.debugMode().get()) {
			enable();
			LOG.debug("Debug mode initialized");
		}
	}

	private void enable() {
		ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
		if (loggerFactory instanceof LoggerContext) {
			LoggerContext context = (LoggerContext) loggerFactory;
			LOGGER_UPGRADES.forEach(loggerUpgrade -> loggerUpgrade.execute(context));
		} else {
			LOG.warn("SLF4J not bound to Logback.");
		}
	}

	private static LoggerUpgrade loggerUpgrade(String loggerName, Level minLevel) {
		return new LoggerUpgrade(loggerName, minLevel);
	}

	private static class LoggerUpgrade {

		private final Level level;
		private final String loggerName;

		public LoggerUpgrade(String loggerName, Level minLevel) {
			this.loggerName = loggerName;
			this.level = minLevel;
		}

		public void execute(LoggerContext context) {
			Logger logger = context.getLogger(loggerName);
			if (logger != null && logger.getEffectiveLevel().isGreaterOrEqual(level)) {
				logger.setLevel(level);
			}
		}

	}

}