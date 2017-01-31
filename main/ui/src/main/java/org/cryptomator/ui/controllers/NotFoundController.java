/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
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
