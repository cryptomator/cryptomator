package org.cryptomator.launcher;

import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
			var i18Dir = Path.of(SupportedLanguages.class.getResource("/i18n").toURI());
			try (var dirStream = Files.newDirectoryStream(i18Dir, "strings_*.properties")) {
				return StreamSupport.stream(dirStream.spliterator(), false) //
						.map(SupportedLanguages::getBCP47CodeFromFileName) //
						.toList();
			}
		} catch (URISyntaxException | IOException e) {
			LOG.warn("Unable to determine additional supported languages.", e);
			return List.of();
		}
	}

	private static String getBCP47CodeFromFileName(Path p) {
		var fileName = p.getFileName().toString();
		return fileName.substring("strings_".length(), fileName.indexOf(".properties")).replace('_', '-');
	}

}
