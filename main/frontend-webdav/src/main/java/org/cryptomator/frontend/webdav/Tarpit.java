/*******************************************************************************
 * Copyright (c) 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.synchronizedSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.cryptomator.frontend.FrontendId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class Tarpit implements Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(Tarpit.class);
	private static final long DELAY_MS = 10000;

	private final Set<FrontendId> validFrontendIds = synchronizedSet(new HashSet<>());

	@Inject
	public Tarpit() {
	}

	public void setValidFrontendIds(Collection<FrontendId> validFrontendIds) {
		this.validFrontendIds.retainAll(validFrontendIds);
		this.validFrontendIds.addAll(validFrontendIds);
	}

	public void handle(HttpServletRequest req) {
		if (isRequestWithInvalidVaultId(req)) {
			delayExecutionUninterruptibly();
			LOG.debug("Delayed request to " + req.getRequestURI() + " by " + DELAY_MS + "ms");
		}
	}

	private boolean isRequestWithInvalidVaultId(HttpServletRequest req) {
		Optional<FrontendId> frontendId = ContextPaths.extractFrontendId(req.getServletPath());
		return frontendId.isPresent() && !isValid(frontendId.get());
	}

	private void delayExecutionUninterruptibly() {
		long expected = currentTimeMillis() + DELAY_MS;
		long sleepTime = DELAY_MS;
		while (expected > currentTimeMillis()) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				sleepTime = max(0, currentTimeMillis() - expected + 10);
			}
		}
	}

	private boolean isValid(FrontendId frontendId) {
		return validFrontendIds.contains(frontendId);
	}

}
