package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.WINDOWS;

@OperatingSystem(WINDOWS)
@CheckAvailability
public final class GoogleDriveWindowsLocationPresetsProvider implements LocationPresetsProvider {

	private static final List<Path> LOCATIONS = Arrays.asList( //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive") //
	);

	@CheckAvailability
	public static boolean isPresent() {
		return LOCATIONS.stream().anyMatch(Files::isDirectory);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		return LOCATIONS.stream() //
				.filter(Files::isDirectory) //
				.map(location -> new LocationPreset("Google Drive", location)) //
				.findFirst() //
				.stream();
	}
}
