package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;

@OperatingSystem(MAC)
@CheckAvailability
public final class DropboxMacLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path LOCATION = LocationPresetsProvider.resolveLocation("~/Library/CloudStorage/Dropbox");
	private static final Path FALLBACK_LOCATION = LocationPresetsProvider.resolveLocation("~/Dropbox");


	@CheckAvailability
	public static boolean isPresent() {
		return Files.isDirectory(LOCATION) || Files.isDirectory(FALLBACK_LOCATION);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		if(Files.isDirectory(LOCATION)) {
			return Stream.of(new LocationPreset("Dropbox", LOCATION));
		} else if(Files.isDirectory(FALLBACK_LOCATION)) {
			return Stream.of(new LocationPreset("Dropbox", FALLBACK_LOCATION));
		} else {
			return Stream.of();
		}
	}
}
