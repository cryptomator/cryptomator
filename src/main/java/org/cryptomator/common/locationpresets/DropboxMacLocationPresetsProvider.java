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

	private static final Path LOCATION1 = LocationPresetsProvider.resolveLocation("~/Library/CloudStorage/Dropbox");
	private static final Path LOCATION2 = LocationPresetsProvider.resolveLocation("~/Dropbox");


	@CheckAvailability
	public static boolean isPresent() {
		return Files.isDirectory(LOCATION1) || Files.isDirectory(LOCATION2);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		if(Files.isDirectory(LOCATION1)) {
			return Stream.of(new LocationPreset("Dropbox", LOCATION1));
		} else if(Files.isDirectory(LOCATION2)) {
			return Stream.of(new LocationPreset("Dropbox", LOCATION2));
		} else {
			return Stream.of();
		}
	}
}
