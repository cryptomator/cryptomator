/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.Parent;

public interface ViewController extends Initializable {

	Parent getRoot();

	@Override
	default void initialize(URL location, ResourceBundle resources) {
		initialize();
	}

	default void initialize() {
		// no-op
	}

	default void focus() {
		// no-op
	}

}
