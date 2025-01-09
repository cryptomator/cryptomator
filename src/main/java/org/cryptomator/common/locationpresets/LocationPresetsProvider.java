package org.cryptomator.common.locationpresets;

import org.cryptomator.common.integrations.IntegrationsLoaderCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface LocationPresetsProvider {

	Logger LOG = LoggerFactory.getLogger(LocationPresetsProvider.class);
	String USER_HOME = System.getProperty("user.home");

	/**
	 * Streams account-separated location presets found by this provider
	 * @return Stream of LocationPresets
	 */
	Stream<LocationPreset> getLocations();

	static Path resolveLocation(String p) {
		if (p.startsWith("~/")) {
			return Path.of(USER_HOME, p.substring(2));
		} else {
			return Path.of(p);
		}
	}

	static Stream<LocationPresetsProvider> loadAll() {
		return IntegrationsLoaderCopy.loadAll(LocationPresetsProvider.class);
	}
}
