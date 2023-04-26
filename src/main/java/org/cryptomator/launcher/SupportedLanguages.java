package org.cryptomator.launcher;

import org.cryptomator.common.settings.Settings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Singleton
public class SupportedLanguages {

	private static final Logger LOG = LoggerFactory.getLogger(SupportedLanguages.class);
	// these are BCP 47 language codes, not ISO. Note the "-" instead of the "_".
	// "en" is not part of this list - it is always inserted at the top.
	public static final List<String> LANGUAGE_TAGS = Arrays.asList("ar", "be", "bn", "bs", "ca", "cs", "da", "de", "el", "es", "fil", "fa", "fr", "gl", "he", //
			"hi", "hr", "hu", "id", "it", "ja", "ko", "lv", "mk", "nb", "nl", "nn", "no", "pa", "pl", "pt", "pt-BR", "ro", "ru", "si", "sk", "sr", "sr-Latn", "sv", "sw", //
			"ta", "te", "th", "tr", "uk", "vi", "zh", "zh-HK", "zh-TW");
	public  static final String ENGLISH = "en";

	@Nullable
	private final String preferredLanguage;

	@Inject
	public SupportedLanguages(Settings settings) {
		this.preferredLanguage = settings.languageProperty().get();
	}

	public void applyPreferred() {
		if (preferredLanguage == null) {
			LOG.debug("Using system locale");
		} else {
			var preferredLocale = Locale.forLanguageTag(preferredLanguage);
			LOG.debug("Applying preferred locale {}", preferredLocale.getDisplayName(Locale.ENGLISH));
			Locale.setDefault(preferredLocale);
		}
		SupportedLanguages.LANGUAGE_TAGS.sort((a, b) -> {
			var collator = Collator.getInstance(Locale.getDefault());
			collator.setStrength(Collator.PRIMARY);
			return collator.compare(Locale.forLanguageTag(a).getDisplayName(), Locale.forLanguageTag(b).getDisplayName());
		});
	}
}
