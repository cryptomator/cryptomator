package org.cryptomator.webdav.jackrabbit;

import java.util.Collection;

class CryptoWarningHandler {

	private final Collection<String> resourcesWithInvalidMac;

	public CryptoWarningHandler(Collection<String> resourcesWithInvalidMac) {
		this.resourcesWithInvalidMac = resourcesWithInvalidMac;
	}

	public void macAuthFailed(String resourceName) {
		if (!resourcesWithInvalidMac.contains(resourceName)) {
			resourcesWithInvalidMac.add(resourceName);
		}
	}

}
