/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Markus Kreusch - Refactored to use strategy pattern
 ******************************************************************************/
package org.cryptomator.ui.util.mount;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebDavMounter {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounter.class);

	private static final WebDavMounterStrategy[] STRATEGIES = {new WindowsWebDavMounter(), new MacOsXWebDavMounter(), new LinuxGvfsWebDavMounter()};

	private static volatile WebDavMounterStrategy choosenStrategy;

	/**
	 * Tries to mount a given webdav share.
	 * 
	 * @param uri URI of the webdav share
	 * @param name the name under which the folder is to be mounted. This might be ignored.
	 * @return a {@link WebDavMount} representing the mounted share
	 * @throws CommandFailedException if the mount operation fails
	 */
	public static WebDavMount mount(URI uri, String name) throws CommandFailedException {
		return chooseStrategy().mount(uri, name);
	}

	private static WebDavMounterStrategy chooseStrategy() {
		if (choosenStrategy == null) {
			choosenStrategy = getStrategyWhichShouldWork();
		}
		return choosenStrategy;
	}

	private static WebDavMounterStrategy getStrategyWhichShouldWork() {
		for (WebDavMounterStrategy strategy : STRATEGIES) {
			if (strategy.shouldWork()) {
				LOG.info("Using {}", strategy.getClass().getSimpleName());
				return strategy;
			}
		}
		return new FallbackWebDavMounter();
	}

	private WebDavMounter() {
		throw new IllegalStateException("Class is not instantiable.");
	}

}
