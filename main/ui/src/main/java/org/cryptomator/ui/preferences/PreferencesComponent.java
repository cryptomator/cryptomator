/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.preferences;

import dagger.Subcomponent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.mainwindow.MainWindowModule;

import java.io.IOException;
import java.io.UncheckedIOException;

@PreferencesScoped
@Subcomponent(modules = {PreferencesModule.class})
public interface PreferencesComponent {

	Stage preferencesWindow();

	FXMLLoaderFactory fxmlLoaders();

	default void showPreferencesWindow() {
		try {
			Parent root = fxmlLoaders().load("/fxml/preferences.fxml").getRoot();
			Stage stage = preferencesWindow();
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load main_window.fxml", e);
		}
	}

	@Subcomponent.Builder
	interface Builder {

		PreferencesComponent build();
	}

}
