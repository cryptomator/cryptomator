/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.fxml.FXMLLoader;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.l10n.Localization;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

@FxApplicationScoped
public class ViewControllerLoader {

	private final Map<Class<? extends ViewController>, Provider<ViewController>> controllerProviders;
	private final Localization localization;

	@Inject
	public ViewControllerLoader(Map<Class<? extends ViewController>, Provider<ViewController>> controllerProviders, Localization localization) {
		this.controllerProviders = controllerProviders;
		this.localization = localization;
	}

	public <T extends ViewController> T load(String resourceName) {
		FXMLLoader loader = new FXMLLoader();
		loader.setControllerFactory(this::constructController);
		loader.setResources(localization);
		try (InputStream in = getClass().getResourceAsStream(resourceName)) {
			loader.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Error loading FXML: " + resourceName, e);
		}
		return loader.getController();
	}

	private ViewController constructController(Class<?> clazz) {
		Provider<ViewController> ctrlProvider = controllerProviders.get(clazz);
		if (ctrlProvider == null) {
			throw new IllegalStateException("No provider for type " + clazz.getName() + " registered.");
		}
		return ctrlProvider.get();
	}

}
