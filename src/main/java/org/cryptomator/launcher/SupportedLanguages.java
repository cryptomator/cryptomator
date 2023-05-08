package org.cryptomator.launcher;

import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

@Singleton
public class SupportedLanguages {

	private static final Logger LOG = LoggerFactory.getLogger(SupportedLanguages.class);

	public static final String ENGLISH = "en";

	private final List<String> sortedLanguageTags;

	private final Locale preferredLocale;

	@Inject
	public SupportedLanguages(Settings settings) {
		var preferredLanguage = settings.languageProperty().get();
		preferredLocale = preferredLanguage == null ? Locale.getDefault() : Locale.forLanguageTag(preferredLanguage);
		var collator = Collator.getInstance(preferredLocale);
		collator.setStrength(Collator.PRIMARY);
		List<String> sortedTags = new ArrayList<>();
		sortedTags.add(0, Settings.DEFAULT_LANGUAGE);
		sortedTags.add(1, ENGLISH);
		getSupportedLanguageTags().stream() //
				.sorted((a, b) -> collator.compare(Locale.forLanguageTag(a).getDisplayName(), Locale.forLanguageTag(b).getDisplayName())) //
				.forEach(sortedTags::add);
		sortedLanguageTags = Collections.unmodifiableList(sortedTags);
	}

	public void applyPreferred() {
		LOG.debug("Using locale {}", preferredLocale);
		Locale.setDefault(preferredLocale);
	}

	public List<String> getLanguageTags() {
		return sortedLanguageTags;
	}


	/**
	 * Iterates over the /i18n directory and extracts from every localization file  the BCP 47 code.
	 *
	 * @return list of supported BCP 47 language codes
	 */
	private static List<String> getSupportedLanguageTags() {
		try {
			var i18DirURI = SupportedLanguages.class.getResource("/i18n").toURI();
			var uriScheme = i18DirURI.getScheme();
			if (uriScheme.equals("jar")) {
				final String[] array = i18DirURI.toString().split("!");
				try (var jarFs = FileSystems.newFileSystem(URI.create(array[0]), Map.<String, String>of())) {
					return streamDirectory(jarFs.getPath(array[1]));
				}
			} else if (uriScheme.equals("file")) {
				return streamDirectory(Path.of(i18DirURI));
			} else {
				throw new IOException("Unsupported uri scheme: " + uriScheme);
			}
		} catch (URISyntaxException | IOException e) {
			LOG.warn("Unable to determine additional supported languages.", e);
			return List.of();
		}
	}

	private static List<String> streamDirectory(Path i18Dir) throws IOException {
		try (var dirStream = Files.newDirectoryStream(i18Dir, "strings_*.properties")) {
			return StreamSupport.stream(dirStream.spliterator(), false) //
					.map(SupportedLanguages::getBCP47CodeFromFileName) //
					.toList();
		}
	}

	private static String getBCP47CodeFromFileName(Path p) {
		var fileName = p.getFileName().toString();
		return fileName.substring("strings_".length(), fileName.indexOf(".properties")).replace('_', '-');
	}

}
