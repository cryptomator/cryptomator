package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;

@OperatingSystem(MAC)
public final class GoogleDriveMacLocationPresetsProvider implements LocationPresetsProvider {
	private static final Path LOCATION = LocationPresetsProvider.resolveLocation("~/Library/CloudStorage/").toAbsolutePath();
	private static final Predicate<String> PATTERN = Pattern.compile("^GoogleDrive-[^/]+$").asMatchPredicate();

	private static final List<Path> FALLBACK_LOCATIONS = Arrays.asList( //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive") //
	);

	@Override
	public Stream<LocationPreset> getLocations() {
		if(isLocationPresent()) {
			return getCloudStorageDirLocations();
		} else if(FALLBACK_LOCATIONS.stream().anyMatch(Files::isDirectory)) {
			return getFallbackLocation();
		} else {
			return Stream.of();
		}
	}

	@CheckAvailability
	public static boolean isPresent() {
		return isLocationPresent() || FALLBACK_LOCATIONS.stream().anyMatch(Files::isDirectory);
	}

	public static boolean isLocationPresent() {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(LOCATION, "GoogleDrive-*")) {
			return stream.iterator().hasNext();
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns Google Drive preset String.
	 *
	 * @param accountPath The path to the Google Drive account directory (e.g. {@code ~/Library/CloudStorage/GoogleDrive-username})
	 * @param drivePath The path to the Google Drive file directory, within the account directory. (e.g. {@code ~/Library/CloudStorage/GoogleDrive-username/drive_name})
	 * @return {@code String}. For example: "Google Drive - username - drive_name"
	 */
	private String getDriveLocationString(Path accountPath, Path drivePath) {
		String accountName = accountPath.getFileName().toString().replace("GoogleDrive-", "");
		String driveName = drivePath.getFileName().toString();

		return STR."Google Drive - \{accountName} - \{driveName}";
	}

	/**
	 * Handles searching through {@code ~/Library/CloudStorage/} for directories with the "{@code GoogleDrive-*}" pattern,
	 * and returns the corresponding presets.
	 *
	 * @return {@code Stream<LocationPreset>}. Displays as "{@code Google Drive - username - drive_name}"
	 */
	private Stream<LocationPreset> getCloudStorageDirLocations() {
		try (var dirStream = Files.list(LOCATION)) {
			var presets = dirStream.filter(path -> Files.isDirectory(path) && PATTERN.test(path.getFileName().toString()))
					.flatMap(accountPath -> {
						try {
							return Files.list(accountPath)
									.filter(Files::isDirectory)
									.map(drivePath -> new LocationPreset(getDriveLocationString(accountPath, drivePath), drivePath));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}).toList();
			return presets.stream();
		}
		catch (IOException | UncheckedIOException e) {
			return Stream.of();
		}
	}

	/**
	 * Uses {@code FALLBACK_LOCATIONS} for directories as fallback, if {@code ~/Library/CloudStorage/} isn't present.
	 * Returns the corresponding presets.
	 *
	 * @return {@code Stream<LocationPreset>}. Displays as "{@code Google Drive}"
	 */
	private Stream<LocationPreset> getFallbackLocation() {
		return FALLBACK_LOCATIONS.stream() //
				.filter(Files::isDirectory) //
				.map(location -> new LocationPreset("Google Drive", location))
				.findFirst()
				.stream();
	}
}
