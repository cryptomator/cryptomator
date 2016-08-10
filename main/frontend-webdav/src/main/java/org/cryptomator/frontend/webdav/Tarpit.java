/*******************************************************************************
 * Copyright (c) 2016 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
class Tarpit {
	
	private static final long DELAY_MS = 10000;
	
	@Inject
	public Tarpit() {}
	
	public void handle(HttpServletRequest req) {
		if (isRequestWithVaultId(req)) {
			delayExecutionUninterruptibly();
		}
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

	private boolean isRequestWithVaultId(HttpServletRequest req) {
		String path = req.getServletPath();
		return path.matches("^/[a-zA-Z0-9_-]{12}/.*$");
	}

}
