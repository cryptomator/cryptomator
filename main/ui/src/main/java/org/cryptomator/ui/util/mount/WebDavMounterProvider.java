/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch - Refactored to use strategy pattern
 *     Sebastian Stenzel - Refactored to use Guice provider, added warmup-phase for windows mounts.
 ******************************************************************************/
package org.cryptomator.ui.util.mount;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;

import org.cryptomator.webdav.WebDavServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavMounterProvider implements Provider<WebDavMounter> {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounterProvider.class);
	private static final WebDavMounterStrategy[] STRATEGIES = {new WindowsWebDavMounter(), new MacOsXWebDavMounter(), new LinuxGvfsWebDavMounter()};
	private final WebDavMounterStrategy choosenStrategy;

	@Inject
	public WebDavMounterProvider(WebDavServer server, ExecutorService executorService) {
		this.choosenStrategy = getStrategyWhichShouldWork();
		executorService.execute(() -> {
			this.choosenStrategy.warmUp(server.getPort());
		});
	}

	@Override
	public WebDavMounterStrategy get() {
		return this.choosenStrategy;
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

}
