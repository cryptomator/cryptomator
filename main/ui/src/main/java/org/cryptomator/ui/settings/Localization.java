package org.cryptomator.ui.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Localization extends ResourceBundle {

	private static final Logger LOG = LoggerFactory.getLogger(Localization.class);

	private static final String LOCALIZATION_DEFAULT_FILE = "/localization/en.txt";
	private static final String LOCALIZATION_FILENAME_FMT = "/localization/%s.txt";

	private final ResourceBundle fallback;
	private final ResourceBundle localized;

	@Inject
	public Localization() {
		try {
			this.fallback = Objects.requireNonNull(loadLocalizationFile(LOCALIZATION_DEFAULT_FILE));
			LOG.debug("Loaded localization default file: {}", LOCALIZATION_DEFAULT_FILE);

			String language = Locale.getDefault().getLanguage();
			String region = Locale.getDefault().getCountry();
			LOG.info("Detected language \"{}\" and region \"{}\"", language, region);

			ResourceBundle localizationBundle = null;
			if (StringUtils.isNotEmpty(language) && StringUtils.isNotEmpty(region)) {
				String file = String.format(LOCALIZATION_FILENAME_FMT, language + "_" + region);
				LOG.info("Attempting to load localization from: {}", file);
				localizationBundle = loadLocalizationFile(file);
			}
			if (StringUtils.isNotEmpty(language) && localizationBundle == null) {
				String file = String.format(LOCALIZATION_FILENAME_FMT, language);
				LOG.info("Attempting to load localization from: {}", file);
				localizationBundle = loadLocalizationFile(file);
			}
			if (localizationBundle == null) {
				LOG.info("No localization found. Falling back to default language.");
				localizationBundle = this.fallback;
			}
			this.localized = Objects.requireNonNull(localizationBundle);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// returns null if no resource for given path
	private ResourceBundle loadLocalizationFile(String resourcePath) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
			if (in != null) {
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
				return new PropertyResourceBundle(reader);
			} else {
				return null;
			}
		}
	}

	@Override
	protected Object handleGetObject(String key) {
		return localized.containsKey(key) ? localized.getObject(key) : fallback.getObject(key);
	}

	@Override
	public Enumeration<String> getKeys() {
		Collection<String> keys = CollectionUtils.union(localized.keySet(), fallback.keySet());
		return Collections.enumeration(keys);
	}

}
