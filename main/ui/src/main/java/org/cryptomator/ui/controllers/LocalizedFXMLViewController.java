/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import org.cryptomator.ui.settings.Localization;

abstract class LocalizedFXMLViewController extends AbstractFXMLViewController {

	protected final Localization localization;

	public LocalizedFXMLViewController(Localization localization) {
		this.localization = localization;
	}

	@Override
	protected Localization getFxmlResourceBundle() {
		return localization;
	}

}
