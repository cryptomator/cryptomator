package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.LINUX;

@OperatingSystem(LINUX)
@CheckAvailability
public final class OneDriveLinuxLocationPresetsProvider implements LocationPresetsProvider {


	private static final Path LOCATION = LocationPresetsProvider.resolveLocation("~/OneDrive");

	@CheckAvailability
	public static boolean isPresent() {
		return Files.isDirectory(LOCATION);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		return Stream.of(new LocationPreset("OneDrive", LOCATION));
	}
}
