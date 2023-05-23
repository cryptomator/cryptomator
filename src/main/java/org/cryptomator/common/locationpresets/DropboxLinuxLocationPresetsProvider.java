package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.LINUX;

@OperatingSystem(LINUX)
public final class DropboxLinuxLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path USER_HOME = LocationPresetsProvider.resolveLocation("~/.").toAbsolutePath();
	private static final Predicate<String> PATTERN = Pattern.compile("Dropbox \\(.+\\)").asMatchPredicate();

	@Override
	public Stream<LocationPreset> getLocations() {
		try (var dirStream = Files.list(USER_HOME)) {
			var presets = dirStream.filter(p -> Files.isDirectory(p) && PATTERN.test(p.getFileName().toString())) //
					.map(p -> new LocationPreset(p.getFileName().toString(), p)) //
					.toList();
			return presets.stream(); //workaround to ensure that the directory stream is always closed
		} catch (IOException | UncheckedIOException e) { //UncheckedIOException thrown by the stream of Files.list()
			return Stream.of();
		}
	}
}
