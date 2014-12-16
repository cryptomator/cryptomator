/*******************************************************************************
 * Copyright (c) 2014 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.ui.util.command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cryptomator.ui.util.mount.CommandFailedException;

public final class Script {
	
	private static final int DEFAULT_TIMEOUT_MILLISECONDS = 3000;
	
	public static Script fromLines(String ... commands) {
		return new Script(commands);
	}
	
	private final String[] lines;
	private final Map<String,String> environment = new HashMap<>();
	private long timeout = DEFAULT_TIMEOUT_MILLISECONDS;
	private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;
	
	private Script(String[] lines) {
		this.lines = lines;
		setEnv(System.getenv());
	}
	
	public String[] getLines() {
		return lines;
	}
	
	public CommandResult execute() throws CommandFailedException {
		return CommandRunner.execute(this);
	}
	
	Map<String,String> environment() {
		return environment;
	}
	
	public Script setEnv(Map<String,String> environment) {
		this.environment.clear();
		addEnv(environment);
		return this;
	}
	
	public Script addEnv(Map<String,String> environment) {
		this.environment.putAll(environment);
		return this;
	}
	
	public Script addEnv(String name, String value) {
		environment.put(name, value);
		return this;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getTimeoutUnit() {
		return timeoutUnit;
	}

	public void setTimeoutUnit(TimeUnit timeoutUnit) {
		this.timeoutUnit = timeoutUnit;
	}
	
}
