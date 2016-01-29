/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Markus Kreusch - Refactored to use strategy pattern
 *     Sebastian Stenzel - Refactored to use Guice provider, added warmup-phase for windows mounts.
 ******************************************************************************/
package org.cryptomator.frontend.webdav.mount;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WebDavMounterProvider implements Provider<WebDavMounter> {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounterProvider.class);
	private final WebDavMounterStrategy choosenStrategy;

	@Inject
	public WebDavMounterProvider(MountStrategies availableStrategies) {
		this.choosenStrategy = getStrategyWhichShouldWork(availableStrategies);
	}

	@Override
	public WebDavMounter get() {
		return this.choosenStrategy;
	}

	private WebDavMounterStrategy getStrategyWhichShouldWork(Collection<WebDavMounterStrategy> availableStrategies) {
		WebDavMounterStrategy strategy = availableStrategies.stream().filter(WebDavMounterStrategy::shouldWork).findFirst().orElse(new FallbackWebDavMounter());
		LOG.info("Using {}", strategy.getClass().getSimpleName());
		return strategy;
	}

}
