package org.cryptomator.ui.controllers;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.settings.Localization;

@Singleton
public class NotFoundController extends LocalizedFXMLViewController {

	@Inject
	public NotFoundController(Localization localization) {
		super(localization);
	}

	@Override
	protected URL getFxmlResourceUrl() {
		return getClass().getResource("/fxml/notfound.fxml");
	}

}
