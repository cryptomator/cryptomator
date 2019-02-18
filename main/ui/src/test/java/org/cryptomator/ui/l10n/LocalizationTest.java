/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.l10n;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalizationTest {

	private static final Logger LOG = LoggerFactory.getLogger(LocalizationTest.class);
	private static final String RESOURCE_FOLDER_PATH = "/localization/";
	private static final String REF_FILE_NAME = "en.txt";
	private static final String[] LANG_FILE_NAMES = {"ar.txt", "bg.txt", "ca.txt", "cs.txt", "da.txt", "de.txt", "es.txt", "fr.txt", "hu.txt", "it.txt", "ja.txt", //
			"ko.txt", "lv.txt", "nl.txt", "pl.txt", "pt.txt", "pt_BR.txt", "ru.txt", "sk.txt", "th.txt", "tr.txt", "uk.txt", "zh_HK.txt", "zh_TW.txt", "zh.txt"};

	/*
	 * @see Formatter
	 */
	private static final String ARG_INDEX_REGEX = "(\\d+\\$)?"; // e.g. %1$s
	private static final String FLAG_REGEX = "[-#+ 0,\\(]*"; // e.g. %0,f
	private static final String WIDTH_AND_PRECISION_REGEX = "(\\d*(\\.\\d+)?)?"; // e.g. %4.2f
	private static final String GENERAL_CONVERSION_REGEX = "[bBhHsScCdoxXeEfgGaA%n]"; // e.g. %f
	private static final String TIME_CONVERSION_REGEX = "[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc]"; // e.g. %1$tY-%1$tm-%1$td
	private static final String CONVERSION_REGEX = "(" + GENERAL_CONVERSION_REGEX + "|" + TIME_CONVERSION_REGEX + ")";
	private static final String PLACEHOLDER_REGEX = "%" + ARG_INDEX_REGEX + FLAG_REGEX + WIDTH_AND_PRECISION_REGEX + CONVERSION_REGEX;
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PLACEHOLDER_REGEX);

	@Test
	public void testStringFormatIsValid() throws IOException {
		ResourceBundle ref = loadLanguage(RESOURCE_FOLDER_PATH + REF_FILE_NAME);
		boolean allGood = true;
		for (String langFileName : LANG_FILE_NAMES) {
			ResourceBundle lang = loadLanguage(RESOURCE_FOLDER_PATH + langFileName);
			allGood &= allStringFormatSpecifiersMatchReferenceLanguage(ref, lang, langFileName);
		}
		Assertions.assertTrue(allGood);
	}

	private boolean allStringFormatSpecifiersMatchReferenceLanguage(ResourceBundle ref, ResourceBundle lang, String langFileName) {
		boolean allGood = true;
		for (String key : Collections.list(ref.getKeys())) {
			if (!lang.containsKey(key)) {
				continue;
			}
			List<String> refPlaceholders = findPlaceholders(ref.getString(key));
			if (refPlaceholders.isEmpty()) {
				continue;
			}
			List<String> langPlaceholders = findPlaceholders(lang.getString(key));
			if (!langPlaceholders.containsAll(refPlaceholders) || !refPlaceholders.containsAll(langPlaceholders)) {
				LOG.warn("Placeholders don't match for term {}. Lang={}, Required={}, Found={}", key, langFileName, refPlaceholders, langPlaceholders);
				allGood = false;
			}
		}
		return allGood;
	}

	private List<String> findPlaceholders(String str) {
		Matcher m = PLACEHOLDER_PATTERN.matcher(str);
		List<String> placeholders = new ArrayList<>();
		while (m.find()) {
			placeholders.add(m.group());
		}
		return placeholders;
	}

	private ResourceBundle loadLanguage(String path) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(path)) {
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			return new PropertyResourceBundle(reader);
		}
	}
}
