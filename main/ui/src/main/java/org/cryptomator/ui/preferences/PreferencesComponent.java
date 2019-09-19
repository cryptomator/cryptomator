/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.preferences;

import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

@PreferencesScoped
@Subcomponent(modules = {PreferencesModule.class})
public interface PreferencesComponent {

	@PreferencesWindow
	Stage window();

	@FxmlScene(FxmlFile.PREFERENCES)
	Lazy<Scene> scene();

	default Stage showPreferencesWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		stage.requestFocus();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		PreferencesComponent build();
	}

}
