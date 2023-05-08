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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class SupportedLanguages {

	private static final Logger LOG = LoggerFactory.getLogger(SupportedLanguages.class);
	// these are BCP 47 language codes, not ISO. Note the "-" instead of the "_":
	public static final List<String> LANGUAGE_TAGS;

	static {
		List<String> supportedLanguages = new ArrayList<>();
		try {
			var i18Dir = getI18Dir();
			try (var dirStream = Files.newDirectoryStream(i18Dir, "strings_*.properties")) {
				StreamSupport.stream(dirStream.spliterator(), false) //
						.map(SupportedLanguages::getBCP47CodeFromFileName) //
						.forEach(supportedLanguages::add);
			}
		} catch (URISyntaxException | IOException e) {
			LOG.warn("Unable to determine additional supported languages.", e);
		}
		LANGUAGE_TAGS = supportedLanguages;
	}

	private static Path getI18Dir() throws URISyntaxException {
		var i18nUri = Optional.of(SupportedLanguages.class.getResource("/i18n")).get().toURI();
		return Path.of(i18nUri);
	}

	private static String getBCP47CodeFromFileName(Path p) {
		var fileName = p.getFileName().toString();
		return fileName.substring("strings_".length(), fileName.indexOf(".properties")).replace('_', '-');
	}

	public static final String ENGLISH = "en";

	private final List<String> sortedLanguageTags;

	private final Locale preferredLocale;

	@Inject
	public SupportedLanguages(Settings settings) {
		var preferredLanguage = settings.languageProperty().get();
		preferredLocale = preferredLanguage == null ? Locale.getDefault() : Locale.forLanguageTag(preferredLanguage);
		var collator = Collator.getInstance(preferredLocale);
		collator.setStrength(Collator.PRIMARY);
		var sorted = LANGUAGE_TAGS.stream() //
				.sorted((a, b) -> collator.compare(Locale.forLanguageTag(a).getDisplayName(), Locale.forLanguageTag(b).getDisplayName())) //
				.collect(Collectors.toList());
		sorted.add(0, Settings.DEFAULT_LANGUAGE);
		sorted.add(1, ENGLISH);
		sortedLanguageTags = Collections.unmodifiableList(sorted);
	}

	public void applyPreferred() {
		LOG.debug("Using locale {}", preferredLocale);
		Locale.setDefault(preferredLocale);
	}

	public List<String> getLanguageTags() {
		return sortedLanguageTags;
	}

}
