/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.l10n;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

@FxApplicationScoped
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
			LOG.debug("Detected language \"{}\" and region \"{}\"", language, region);

			ResourceBundle localizationBundle = null;
			if (StringUtils.isNotEmpty(language) && StringUtils.isNotEmpty(region)) {
				String file = String.format(LOCALIZATION_FILENAME_FMT, language + "_" + region);
				LOG.trace("Attempting to load localization from: {}", file);
				localizationBundle = loadLocalizationFile(file);
			}
			if (StringUtils.isNotEmpty(language) && localizationBundle == null) {
				String file = String.format(LOCALIZATION_FILENAME_FMT, language);
				LOG.trace("Attempting to load localization from: {}", file);
				localizationBundle = loadLocalizationFile(file);
			}
			if (localizationBundle == null) {
				LOG.debug("No localization found. Falling back to default language.");
				localizationBundle = this.fallback;
			}
			this.localized = Objects.requireNonNull(localizationBundle);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// returns null if no resource for given path
	private static ResourceBundle loadLocalizationFile(String resourcePath) throws IOException {
		try (InputStream in = Localization.class.getResourceAsStream(resourcePath)) {
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
		Collection<String> keys = Sets.union(localized.keySet(), fallback.keySet());
		return Collections.enumeration(keys);
	}

}
