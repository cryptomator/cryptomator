/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.cryptomator.common.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SettingsTest {

	@Test
	public void testAutoSave() {
		Environment env = Mockito.mock(Environment.class);
		SettingsProvider provider = Mockito.mock(SettingsProvider.class);

		Settings settings = Settings.create(provider, env);
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		Mockito.verify(provider, Mockito.times(0)).scheduleSave(settings);

		// first change (to property):
		settings.port.set(42428);
		Mockito.verify(provider, Mockito.times(1)).scheduleSave(settings);

		// second change (to list):
		settings.directories.add(vaultSettings);
		Mockito.verify(provider, Mockito.times(2)).scheduleSave(settings);

		// third change (to property of list item):
		vaultSettings.displayName.set("asd");
		Mockito.verify(provider, Mockito.times(3)).scheduleSave(settings);
	}

}
