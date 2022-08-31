package org.cryptomator.logging;

public class LogbackConfiguratorFactory {

	public static LogbackConfigurator provider() {
		final class Holder {
			private static final LogbackConfigurator INSTANCE = new LogbackConfigurator();
		}
		return Holder.INSTANCE;
	}

}
