package org.cryptomator.logging;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import ch.qos.logback.core.util.FileSize;

import java.io.File;

/**
 * Triggers a roll-over either on the first log event or if watched log file reaches a certain size
 *
 * @param <E> Event type the policy possibly reacts to
 */
public class LaunchAndSizeBasedTriggeringPolicy<E> extends TriggeringPolicyBase<E> {

	LaunchBasedTriggeringPolicy<E> launchBasedTriggeringPolicy;
	SizeBasedTriggeringPolicy<E> sizeBasedTriggeringPolicy;

	public LaunchAndSizeBasedTriggeringPolicy(FileSize threshold) {
		this.launchBasedTriggeringPolicy = new LaunchBasedTriggeringPolicy<>();
		this.sizeBasedTriggeringPolicy = new SizeBasedTriggeringPolicy<>();
		sizeBasedTriggeringPolicy.setMaxFileSize(threshold);
	}

	@Override
	public boolean isTriggeringEvent(File activeFile, E event) {
		return launchBasedTriggeringPolicy.isTriggeringEvent(activeFile, event) || sizeBasedTriggeringPolicy.isTriggeringEvent(activeFile, event);
	}

}
