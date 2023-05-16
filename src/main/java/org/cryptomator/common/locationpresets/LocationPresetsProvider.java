package org.cryptomator.common.locationpresets;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface LocationPresetsProvider {

	String USER_HOME = System.getProperty("user.home");

	Stream<LocationPreset> getLocations();

	static Path resolveLocation(String p) {
		if (p.startsWith("~/")) {
			return Path.of(USER_HOME, p.substring(2));
		} else {
			return Path.of(p);
		}
	}

}
