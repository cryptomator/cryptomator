package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.OperatingSystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.cryptomator.integrations.common.OperatingSystem.Value.MAC;

@OperatingSystem(MAC)
@CheckAvailability
public final class GoogleDriveMacLocationPresetsProvider implements LocationPresetsProvider {

	private static final Path ROOT_LOCATION = LocationPresetsProvider.resolveLocation("~/Library/CloudStorage/").toAbsolutePath();
	private static final Predicate<String> PATTERN = Pattern.compile("^GoogleDrive-[^/]+$").asMatchPredicate();

	private static final List<Path> FALLBACK_LOCATIONS = Arrays.asList( //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive/My Drive"), //
			LocationPresetsProvider.resolveLocation("~/GoogleDrive"), //
			LocationPresetsProvider.resolveLocation("~/Google Drive") //
	);

	@Override
	public Stream<LocationPreset> getLocations() {
		if (isRootLocationPresent()) {
			return getCloudStorageDirLocations();
		} else {
			return getFallbackLocation();
		}
	}

	@CheckAvailability
	public static boolean isPresent() {
		return isRootLocationPresent() || FALLBACK_LOCATIONS.stream().anyMatch(Files::isDirectory);
	}

	/**
	 * Checks if a root location directory is present that matches the specified pattern.
	 * <p>
	 * This method scans the `ROOT_LOCATION` directory for subdirectories and tests each one against a pre-defined pattern.
	 * The method returns `true` if at least one directory is found that satisfies the pattern condition, and `false` otherwise.
	 *
	 * @return {@code true} if a matching root location is present, otherwise {@code false}.
	 * @throws UncheckedIOException if an I/O error occurs during directory traversal.
	 */
	public static boolean isRootLocationPresent() {
		try (var dirStream = Files.list(ROOT_LOCATION)) {
			return dirStream.anyMatch(path -> Files.isDirectory(path) && PATTERN.test(path.getFileName().toString()));
		} catch (IOException | UncheckedIOException e) {
			return false;
		}
	}

	/**
	 * Returns Google Drive preset String.
	 *
	 * @param accountPath The path to the Google Drive account directory (e.g. {@code ~/Library/CloudStorage/GoogleDrive-username})
	 * @return {@code String}. For example: "Google Drive - username"
	 */
	private String getDriveLocationString(Path accountPath, Path drivePath) {
		String accountName = accountPath.getFileName().toString().replace("GoogleDrive-", "");
		return STR."Google Drive - \{accountName}";
	}

	/**
	 * Retrieves a stream of `LocationPreset` objects from directories within the `ROOT_LOCATION` path.
	 * <p>
	 * This method lists all directories in the `ROOT_LOCATION`, filters them based on whether their names match
	 * a predefined pattern (`PATTERN`), and then extracts presets using `getPresetsFromAccountPath(Path)`.
	 * The results are collected into a list and returned as a stream.
	 * <p>
	 *
	 * @return a stream of `LocationPreset` objects representing valid cloud storage directory locations.
	 */
	private Stream<LocationPreset> getCloudStorageDirLocations() {
		try (var dirStream = Files.list(ROOT_LOCATION)) {
			return dirStream.filter(path -> Files.isDirectory(path) && PATTERN.test(path.getFileName().toString())).flatMap(this::getPresetsFromAccountPath).toList().stream();
		} catch (IOException | UncheckedIOException e) {
			return Stream.empty();
		}
	}

	/**
	 * Retrieves a stream of `LocationPreset` objects from a given Google Drive account path.
	 * <p>
	 * This method lists all directories within the provided `accountPath` and filters them
	 * to identify folders whose names match any of the translations defined in `MY_DRIVE_TRANSLATIONS`.
	 * <p>
	 * Each matching folder is then converted into a `LocationPreset` object.
	 *
	 * @param accountPath the root path of the Google Drive account to scan.
	 * @return a stream of `LocationPreset` objects representing matching directories.
	 */
	private Stream<LocationPreset> getPresetsFromAccountPath(Path accountPath) {
		try (var driveStream = Files.list(accountPath)) {
			List<Path> directories = StreamSupport.stream(driveStream.spliterator(), false).toList();
			return directories.stream()
					.filter(preset -> MY_DRIVE_TRANSLATIONS
							.contains(preset.getFileName().toString()))
					.map(drivePath -> new LocationPreset(
							getDriveLocationString(accountPath, drivePath),
							drivePath
					));
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	/**
	 * Returns a stream containing a fallback location preset for Google Drive.
	 * <p>
	 * This method iterates through the predefined fallback locations, checks if any of them is a directory,
	 * and creates a `LocationPreset` object for the first matching directory found. The resulting stream contains
	 * either one preset (if a matching directory is found) or is empty.
	 * <p>
	 * This method is marked as {@code @Deprecated} because it is intended for legacy support only and may be removed in future versions.
	 *
	 * @return a stream containing a single fallback location preset if a valid directory is found, otherwise an empty stream.
	 * @deprecated This method is intended for legacy support and may be removed in future releases.
	 */
	@Deprecated
	private Stream<LocationPreset> getFallbackLocation() {
		return FALLBACK_LOCATIONS.stream() //
				.filter(Files::isDirectory) //
				.map(location -> new LocationPreset("Google Drive", location))
				.findFirst()
				.stream();
	}

	/**
	 * Set of translations for "My Drive" in various languages.
	 * <p>
	 * This constant is used to identify different language-specific labels for "My Drive" in Google Drive.
	 * By using a {@link HashSet}, duplicate entries are automatically removed, ensuring a unique set of translations.
	 * <p>
	 * The translations were originally extracted from the Chromium project’s Chrome OS translation files.
	 * <p>
	 * Source: `ui/chromeos/translations` directory in the Chromium repository.
	 */
	private static final Set<String> MY_DRIVE_TRANSLATIONS = new HashSet<>(List.of("My Drive", "የእኔ Drive", "ملفاتي", "মোৰ ড্ৰাইভ", "Diskim", "Мой Дыск", "Моят диск", "আমার ড্রাইভ", "Moj disk", "La meva unitat", "Můj disk", "My Drive", "Mit drev", "Meine Ablage", "Το Drive μου", "My Drive", "Mi unidad", "Mi unidad", "Minu ketas", "Nire unitatea", "My Drive", "Aking Drive", "Oma Drive", "Mon disque", "Mon Drive", "A miña unidade", "મારી ડ્રાઇવ", "मेरी ड्राइव", "Moj disk", "Saját meghajtó", "Իմ դրայվը", "Drive Saya", "Drifið mitt", "I miei file", "האחסון שלי", "マイドライブ", "ჩემი Drive", "Менің Drive дискім", "ដ្រាយរបស់ខ្ញុំ", "ನನ್ನ ಡ್ರೈವ್", "내 드라이브", "Менин Drive'ым", "My Drive", "Mano Diskas", "Mans disks", "Мојот Drive", "എന്റെ ഡ്രൈവ്", "Миний Драйв", "माझा ड्राइव्ह", "Drive Saya", "My Drive", "मेरो ड्राइभ", "Mijn Drive", "Min disk", "ମୋ ଡ୍ରାଇଭ୍", "My Drive", "Mój dysk", "Meu Drive", "O meu disco", "Contul meu Drive", "Мой диск", "මගේ Drive", "Môj disk", "Moj disk", "Disku im", "Moj disk", "Мој диск", "Min enhet", "Hifadhi Yangu", "எனது இயக்ககம்", "నా డ్రైవ్‌", "ไดรฟ์ของฉัน", "Drive'ım", "Мій диск", "میری ڈرائیو", "My Drive", "Drive của tôi", "我的云端硬盘", "我的雲端硬碟", "我的雲端硬碟", "IDrayivu yami"));
}
