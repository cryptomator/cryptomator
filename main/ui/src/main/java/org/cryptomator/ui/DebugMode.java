package org.cryptomator.ui;

import static java.util.Arrays.asList;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DebugMode {

	private static final Logger LOG = LoggerFactory.getLogger(DebugMode.class);

	private static final Collection<LoggerUpgrade> LOGGER_UPGRADES = asList( //
			loggerUpgrade(ROOT_LOGGER_NAME, Level.INFO), //
			loggerUpgrade("org.cryptomator", Level.TRACE), //
			loggerUpgrade("org.eclipse.jetty.server.Server", Level.DEBUG) //
	);

	private final Settings settings;

	@Inject
	public DebugMode(Settings settings) {
		this.settings = settings;
	}

	public void initialize() {
		if (settings.getDebugMode()) {
			enable();
			LOG.debug("Debug mode initialized");
		}
	}

	private void enable() {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		LOGGER_UPGRADES.forEach(loggerUpgrade -> loggerUpgrade.execute(config));
		context.updateLoggers();
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

		public void execute(Configuration config) {
			LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
			if (loggerConfig.getLevel().isMoreSpecificThan(level)) {
				loggerConfig.setLevel(level);
			}
		}

	}

}