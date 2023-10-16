package org.cryptomator.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.FileSize;
import org.cryptomator.common.Environment;

import java.nio.file.Path;
import java.util.Map;

@ConfiguratorRank(ConfiguratorRank.CUSTOM_NORMAL_PRIORITY)
public class LogbackConfigurator extends ContextAwareBase implements Configurator {

	private static final String LOG_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
	private static final String UPGRADE_FILENAME = "upgrade.log";
	private static final String LOGFILE_NAME = "cryptomator0.log";
	private static final String LOGFILE_ROLLING_PATTERN = "cryptomator%i.log";
	private static final int LOGFILE_ROLLING_MIN = 1;
	private static final int LOGFILE_ROLLING_MAX = 9;
	private static final String LOG_MAX_SIZE = "100mb";

	static final Map<String, Level> DEFAULT_LOG_LEVELS = Map.of( //
			Logger.ROOT_LOGGER_NAME, Level.INFO, //
			"org.cryptomator", Level.INFO //
	);
	static final Map<String, Level> DEBUG_LOG_LEVELS = Map.of( //
			Logger.ROOT_LOGGER_NAME, Level.INFO, //
			"org.cryptomator", Level.TRACE //
	);

	LogbackConfigurator() {}

	/**
	 * Adjust the log levels
	 *
	 * @param logLevels new log levels to use
	 */
	void setLogLevels(Map<String, Level> logLevels) {
		if (context instanceof LoggerContext lc) {
			for (var loglevel : logLevels.entrySet()) {
				Logger logger = lc.getLogger(loglevel.getKey());
				logger.setLevel(loglevel.getValue());
			}
		}
	}

	@Override
	public ExecutionStatus configure(LoggerContext context) {
		var useCustomCfg = Environment.getInstance().useCustomLogbackConfig();
		var logDir = Environment.getInstance().getLogDir().orElse(null);

		if (useCustomCfg) {
			addInfo("Using external logback configuration file.");
		} else {
			// configure appenders:
			var stdout = stdOutAppender(context);
			var noop = noopAppender(context);
			var file = logDir == null ? noop : fileAppender(context, logDir);
			var upgrade = logDir == null ? noop : upgradeAppender(context, logDir);

			// configure loggers:
			for (var loglevel : DEFAULT_LOG_LEVELS.entrySet()) {
				Logger logger = context.getLogger(loglevel.getKey());
				logger.setLevel(loglevel.getValue());
				logger.setAdditive(false);
				logger.addAppender(stdout);
				logger.addAppender(file);
			}

			// configure upgrade logger:
			Logger upgrades = context.getLogger("org.cryptomator.cryptofs.migration");
			upgrades.setLevel(Level.DEBUG);
			upgrades.addAppender(stdout);
			upgrades.addAppender(upgrade);
			upgrades.addAppender(file);
			upgrades.setAdditive(false);

			// configure fuse file locking logger:
			Logger fuseLocking = context.getLogger("org.cryptomator.frontend.fuse.locks");
			fuseLocking.setLevel(Level.OFF);
		}
		return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
	}

	private Appender<ILoggingEvent> noopAppender(LoggerContext context) {
		var appender = new NOPAppender<ILoggingEvent>();
		appender.setContext(context);
		return appender;
	}

	private Appender<ILoggingEvent> stdOutAppender(LoggerContext context) {
		var appender = new ConsoleAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("STDOUT");
		appender.setEncoder(encoder(context));
		appender.start();
		return appender;
	}

	private Appender<ILoggingEvent> upgradeAppender(LoggerContext context, Path logDir) {
		var appender = new FileAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("UPGRADE");
		appender.setFile(logDir.resolve(UPGRADE_FILENAME).toString());
		appender.setEncoder(encoder(context));
		appender.start();
		return appender;
	}

	private Appender<ILoggingEvent> fileAppender(LoggerContext context, Path logDir) {
		var appender = new RollingFileAppender<ILoggingEvent>();
		appender.setContext(context);
		appender.setName("FILE");
		appender.setFile(logDir.resolve(LOGFILE_NAME).toString());
		appender.setEncoder(encoder(context));
		var triggeringPolicy = new LaunchAndSizeBasedTriggeringPolicy<ILoggingEvent>(FileSize.valueOf(LOG_MAX_SIZE));
		triggeringPolicy.setContext(context);
		triggeringPolicy.start();
		appender.setTriggeringPolicy(triggeringPolicy);
		var rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setContext(context);
		rollingPolicy.setFileNamePattern(logDir.resolve(LOGFILE_ROLLING_PATTERN).toString());
		rollingPolicy.setMinIndex(LOGFILE_ROLLING_MIN);
		rollingPolicy.setMaxIndex(LOGFILE_ROLLING_MAX);
		rollingPolicy.setParent(appender);
		rollingPolicy.start();
		appender.setRollingPolicy(rollingPolicy);
		appender.start();
		return appender;
	}

	private PatternLayoutEncoder encoder(LoggerContext context) {
		var encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern(LOG_PATTERN);
		encoder.start();
		return encoder;
	}
}
