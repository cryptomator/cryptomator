package org.cryptomator.launcher;

import org.cryptomator.common.settings.Settings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Locale;

@Singleton
public class SupportedLanguages {

	private static final Logger LOG = LoggerFactory.getLogger(SupportedLanguages.class);
	// these are BCP 47 language codes, not ISO. Note the "-" instead of the "_":
	public static final List<String> LANGUAGAE_TAGS = List.of("en", "ar", "bn", "bs", "ca", "cs", "de", "el", "es", "fil", "fr", "gl", "he", //
			"hi", "hr", "hu", "id", "it", "ja", "ko", "lv", "mk", "nb", "nl", "nn", "no", "pa", "pl", "pt", "pt-BR", "ro", "ru", "sk", "sr", //
			"sr-Latn", "sv", "ta", "te", "th", "tr", "uk", "zh", "zh-HK", "zh-TW");

	@Nullable
	private final String preferredLanguage;

	@Inject
	public SupportedLanguages(Settings settings) {
		this.preferredLanguage = settings.languageProperty().get();
	}

	public void applyPreferred() {
		if (preferredLanguage == null) {
			LOG.debug("Using system locale");
			return;
		}
		var preferredLocale = Locale.forLanguageTag(preferredLanguage);
		LOG.debug("Applying preferred locale {}", preferredLocale.getDisplayName(Locale.ENGLISH));
		Locale.setDefault(preferredLocale);
	}
}
