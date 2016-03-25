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
