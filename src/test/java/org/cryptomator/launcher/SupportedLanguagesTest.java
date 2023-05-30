package org.cryptomator.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class SupportedLanguagesTest {

	@DisplayName("test if resource bundle is localized")
	@ParameterizedTest(name = "{0}")
	@MethodSource("languageTags")
	public void testResourceBundleExists(String tag) {
		var locale = Locale.forLanguageTag(tag);
		Assertions.assertNotEquals("und", locale.toLanguageTag(), "Undefined language tag");

		var bundle = Assertions.assertDoesNotThrow(() -> ResourceBundle.getBundle("i18n.strings", locale));

		Assertions.assertEquals(locale, bundle.getLocale());
		Assertions.assertFalse(bundle.keySet().isEmpty());
	}

	public static Stream<String> languageTags() {
		return SupportedLanguages.LANGUAGE_TAGS.stream() //
				.filter(tag -> !"en".equals(tag)); // english uses the default bundle
	}
}