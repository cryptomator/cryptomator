/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import org.cryptomator.common.FxApplicationScoped;

import javax.inject.Inject;

@FxApplicationScoped
public class NotFoundController implements ViewController {

	@Inject
	public NotFoundController() {
		// no-op
	}

	@FXML
	VBox root;

	@Override
	public Parent getRoot() {
		return root;
	}

}
