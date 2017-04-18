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

}
