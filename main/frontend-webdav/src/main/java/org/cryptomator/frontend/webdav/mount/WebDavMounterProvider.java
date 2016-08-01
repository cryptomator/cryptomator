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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.frontend.Frontend.MountParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WebDavMounterProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavMounterProvider.class);
	private final Collection<WebDavMounterStrategy> availableStrategies;
	private final WebDavMounterStrategy fallbackStrategy;

	@Inject
	public WebDavMounterProvider(Set<WebDavMounterStrategy> availableStrategies, @Named("fallback") WebDavMounterStrategy fallbackStrategy) {
		this.availableStrategies = availableStrategies;
		this.fallbackStrategy = fallbackStrategy;
	}

	public WebDavMounter chooseMounter(Map<MountParam, Optional<String>> mountParams) {
		WebDavMounterStrategy result = availableStrategies.stream().filter(strategy -> strategy.shouldWork(mountParams)).findFirst().orElse(fallbackStrategy);
		LOG.info("Using {}", result.getClass().getSimpleName());
		return result;
	}

}
