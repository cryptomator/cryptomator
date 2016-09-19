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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Localization extends ResourceBundle {

	private static final Logger LOG = LoggerFactory.getLogger(Localization.class);

	private static final String LOCALIZATION_DEFAULT_FILE = "/localization/en.txt";
	private static final String LOCALIZATION_FILENAME_FMT = "/localization/%s.txt";
	private static final String LOCALIZATION_FILE = String.format(LOCALIZATION_FILENAME_FMT, Locale.getDefault().getLanguage());

	private final ResourceBundle fallback;
	private final ResourceBundle localized;

	@Inject
	public Localization() {
		try (InputStream in = getClass().getResourceAsStream(LOCALIZATION_DEFAULT_FILE)) {
			Objects.requireNonNull(in);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			this.fallback = new PropertyResourceBundle(reader);
			LOG.info("Loaded localization from bundle:{}", LOCALIZATION_FILE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (InputStream in = getClass().getResourceAsStream(LOCALIZATION_FILE)) {
			if (in != null) {
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
				this.localized = new PropertyResourceBundle(reader);
			} else {
				this.localized = this.fallback;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
