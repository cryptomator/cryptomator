package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.cryptomator.integrations.common.OperatingSystem.Value.LINUX;

@OperatingSystem(LINUX)
public final class DropboxLinuxLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path USER_HOME = LocationPresetsProvider.resolveLocation("~/.").toAbsolutePath();
	private static final Pattern PATTERN = Pattern.compile("Dropbox \\(.+\\)");

	@Override
	public Stream<LocationPreset> getLocations() {
		try (var dirStream = Files.newDirectoryStream(USER_HOME,"Dropbox*")){
			return StreamSupport.stream(dirStream.spliterator(), false).flatMap(p -> {
				var matcher = PATTERN.matcher(p.getFileName().toString());
				if(matcher.matches() && Files.isDirectory(p)) {
					return Stream.of(new LocationPreset(matcher.group(), p));
				} else {
					return Stream.of();
				}
			});
		} catch (IOException e) {
			return Stream.of();
		}
	}
}
