package org.cryptomator.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.util.Duration;
import org.cryptomator.common.Environment;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class LoggerConfiguration {

	private static final double SHUTDOWN_DELAY_MS = 100;

	private final LoggerContext context;
	private final Environment environment;
	private final Appender<ILoggingEvent> stdout;
	private final Appender<ILoggingEvent> upgrade;
	private final Appender<ILoggingEvent> file;

	@Inject
	LoggerConfiguration(LoggerContext context, //
						Environment environment, //
						@Named("stdoutAppender") Appender<ILoggingEvent> stdout, //
						@Named("upgradeAppender") Appender<ILoggingEvent> upgrade, //
						@Named("fileAppender") Appender<ILoggingEvent> file) {
		this.context = context;
		this.environment = environment;
		this.stdout = stdout;
		this.upgrade = upgrade;
		this.file = file;
	}

	public void init() {
		if (environment.useCustomLogbackConfig()) {
			Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
			root.info("Using external logback configuration file.");
		} else {
			context.reset();

			// configure loggers:
			for (Map.Entry<String, Level> loglevel : LoggerModule.DEFAULT_LOG_LEVELS.entrySet()) {
				Logger logger = context.getLogger(loglevel.getKey());
				logger.setLevel(loglevel.getValue());
				logger.setAdditive(false);
				logger.addAppender(stdout);
				logger.addAppender(file);
			}

			// configure upgrade logger:
			Logger upgrades = context.getLogger("org.cryptomator.ui.model.upgrade");
			upgrades.setLevel(Level.DEBUG);
			upgrades.addAppender(stdout);
			upgrades.addAppender(upgrade);
			upgrades.setAdditive(false);

			// add shutdown hook
			DelayingShutdownHook shutdownHook = new DelayingShutdownHook();
			shutdownHook.setContext(context);
			shutdownHook.setDelay(Duration.buildByMilliseconds(SHUTDOWN_DELAY_MS));
		}
	}

}
