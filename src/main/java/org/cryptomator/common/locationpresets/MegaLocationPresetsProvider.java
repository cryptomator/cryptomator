package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;
import static org.cryptomator.integrations.common.OperatingSystem.Value.WINDOWS;

@OperatingSystem(WINDOWS)
@OperatingSystem(MAC)
@CheckAvailability
public final class MegaLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path LOCATION = LocationPresetsProvider.resolveLocation("~/MEGA");

	@CheckAvailability
	public static boolean isPresent() {
		return Files.isDirectory(LOCATION);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		return Stream.of(new LocationPreset("MEGA", LOCATION));
	}
}
