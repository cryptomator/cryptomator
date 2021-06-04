/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VaultSettingsTest {

	@ParameterizedTest
	@CsvSource({"a\u000Fa,a_a", ": \\,_ _", "汉语,汉语", "..,_", "a\ta,a\u0020a", "\t\n\r,_"})
	public void testNormalize(String test, String expected) {
		VaultSettings settings = new VaultSettings("id");
		settings.displayName().setValue(test);
		assertEquals(expected, settings.normalizeDisplayName());
	}

}
