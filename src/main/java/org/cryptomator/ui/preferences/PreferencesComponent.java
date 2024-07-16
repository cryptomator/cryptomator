/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.preferences;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

@PreferencesScoped
@Subcomponent(modules = {PreferencesModule.class})
public interface PreferencesComponent {

	@PreferencesWindow
	Stage window();

	@FxmlScene(FxmlFile.PREFERENCES)
	Lazy<Scene> scene();

	ObjectProperty<SelectedPreferencesTab> selectedTabProperty();

	default Stage showPreferencesWindow(SelectedPreferencesTab selectedTab) {
		selectedTabProperty().set(selectedTab);
		Stage stage = window();
		stage.setScene(scene().get());
		stage.setMinWidth(420);
		stage.setMinHeight(300);
		stage.show();
		stage.requestFocus();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		PreferencesComponent build();
	}

}
