/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.preferences;

import dagger.Subcomponent;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;

@PreferencesScoped
@Subcomponent(modules = {PreferencesModule.class})
public interface PreferencesComponent {

	@PreferencesWindow
	Stage window();

	@PreferencesWindow
	FXMLLoaderFactory fxmlLoaders();

	default void showPreferencesWindow() {
		Stage stage = window();
		fxmlLoaders().setScene("/fxml/preferences.fxml", stage);
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		PreferencesComponent build();
	}

}
