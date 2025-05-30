package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;
import static org.cryptomator.integrations.common.OperatingSystem.Value.WINDOWS;

@OperatingSystem(WINDOWS)
@OperatingSystem(MAC)
@CheckAvailability
public final class PCloudLocationPresetsProvider implements LocationPresetsProvider {

	private static final List<Path> LOCATIONS = Arrays.asList( //
			LocationPresetsProvider.resolveLocation("~/pCloudDrive"), //
			LocationPresetsProvider.resolveLocation("~/pCloud Drive") //
	);

	@CheckAvailability
	public static boolean isPresent() {
		return LOCATIONS.stream().anyMatch(Files::isDirectory);
	}

	@Override
	public Stream<LocationPreset> getLocations() {
		return LOCATIONS.stream() //
				.filter(Files::isDirectory) //
				.map(location -> new LocationPreset("pCloud", location)) //
				.findFirst() //
				.stream();
	}

}
