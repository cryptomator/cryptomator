package org.cryptomator.webdav.jackrabbit;

import java.util.Collection;

class CryptoWarningHandler {

	private final Collection<String> resourcesWithInvalidMac;
	private final Collection<String> whitelistedResources;

	public CryptoWarningHandler(Collection<String> resourcesWithInvalidMac, Collection<String> whitelistedResources) {
		this.resourcesWithInvalidMac = resourcesWithInvalidMac;
		this.whitelistedResources = whitelistedResources;
	}

	public void macAuthFailed(String resourcePath) {
		// collection might be a list, but we don't want duplicates:
		if (!resourcesWithInvalidMac.contains(resourcePath)) {
			resourcesWithInvalidMac.add(resourcePath);
		}
	}

	public boolean ignoreMac(String resourcePath) {
		return whitelistedResources.contains(resourcePath);
	}

}
