/*******************************************************************************
 * Copyright (c) 2014, 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount.command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.cryptomator.frontend.CommandFailedException;

public final class Script {

	private static final int DEFAULT_TIMEOUT_MILLISECONDS = 3000;

	public static Script fromLines(String... commands) {
		return new Script(commands);
	}

	private final String[] lines;
	private final Map<String, String> environment = new HashMap<>();

	private Script(String[] lines) {
		this.lines = lines;
		setEnv(System.getenv());
	}

	public String[] getLines() {
		return lines;
	}

	public CommandResult execute() throws CommandFailedException {
		return CommandRunner.execute(this, DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
	}

	public CommandResult execute(long timeout, TimeUnit unit) throws CommandFailedException {
		return CommandRunner.execute(this, timeout, unit);
	}

	Map<String, String> environment() {
		return environment;
	}

	public Script setEnv(Map<String, String> environment) {
		this.environment.clear();
		addEnv(environment);
		return this;
	}

	public Script addEnv(Map<String, String> environment) {
		this.environment.putAll(environment);
		return this;
	}

	public Script addEnv(String name, String value) {
		environment.put(name, value);
		return this;
	}

}
