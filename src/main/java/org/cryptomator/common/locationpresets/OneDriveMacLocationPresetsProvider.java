package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;

@OperatingSystem(MAC)
public final class OneDriveMacLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path FALLBACK_LOCATION = LocationPresetsProvider.resolveLocation("~/OneDrive");
	private static final Path PARENT_LOCATION = LocationPresetsProvider.resolveLocation("~/Library/CloudStorage");

	@Override
	public Stream<LocationPreset> getLocations() {
		var newLocations = getNewLocations().toList();
		if (newLocations.size() >= 1) {
			return newLocations.stream();
		} else {
			return getOldLocation();
		}
	}

	private Stream<LocationPreset> getNewLocations() {
		try (var dirStream = Files.newDirectoryStream(PARENT_LOCATION, "OneDrive*")) {
			return StreamSupport.stream(dirStream.spliterator(), false) //
					.filter(Files::isDirectory) //
					.map(p -> new LocationPreset(String.join(" - ", p.getFileName().toString().split("-")), p));
		} catch (IOException e) {
			return Stream.of();
		}
	}

	private Stream<LocationPreset> getOldLocation() {
		return Stream.of(new LocationPreset("OneDrive", FALLBACK_LOCATION)).filter(preset -> Files.isDirectory(preset.path()));
	}


}
