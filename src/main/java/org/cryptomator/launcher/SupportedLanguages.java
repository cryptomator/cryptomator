package org.cryptomator.launcher;

import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Singleton
public class SupportedLanguages {

	private static final Logger LOG = LoggerFactory.getLogger(SupportedLanguages.class);
	// these are BCP 47 language codes, not ISO. Note the "-" instead of the "_".
	// "en" is not part of this list - it is always inserted at the top.
	public static final List<String> LANGUAGE_TAGS = List.of("ar", "be", "bn", "bs", "ca", "cs", "da", "de", "el", "es", "fr", "gl", "he", //
			"hi", "hr", "hu", "id", "it", "ja", "ko", "lv", "nb", "nl", "nn", "pa", "pl", "pt", "pt-BR", "ro", "ru", "sk", "sr", "sr-Latn", "sv", "sw", //
			"ta", "th", "tr", "uk", "vi", "zh", "zh-HK", "zh-TW");
	public static final String ENGLISH = "en";

	private final List<String> sortedLanguageTags;

	private final Locale preferredLocale;

	@Inject
	public SupportedLanguages(Settings settings) {
		var preferredLanguage = settings.language.get();
		preferredLocale = preferredLanguage == null ? Locale.getDefault() : Locale.forLanguageTag(preferredLanguage);
		var collator = Collator.getInstance(preferredLocale);
		collator.setStrength(Collator.PRIMARY);
		var sorted = new ArrayList<String>();
		sorted.add(0, null);
		sorted.add(1, ENGLISH);
		LANGUAGE_TAGS.stream() //
				.sorted((a, b) -> collator.compare(Locale.forLanguageTag(a).getDisplayName(), Locale.forLanguageTag(b).getDisplayName())) //
				.forEach(sorted::add);
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
