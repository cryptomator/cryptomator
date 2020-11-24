/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.quit;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

@QuitScoped
@Subcomponent(modules = {QuitModule.class})
public interface QuitComponent {

	@QuitWindow
	Stage window();

	@FxmlScene(FxmlFile.QUIT)
	Lazy<Scene> scene();

	ObjectProperty<Stage> provideStageProperty();

	default Stage showQuitWindow() {
		var stageProperty = provideStageProperty();
		Stage stage;
		if (stageProperty.isNull().get()) {
			stage = window();
			stage.setScene(scene().get());
			stageProperty.set(stage);
		} else {
			stage = stageProperty.get();
		}
		stage.show();
		stage.requestFocus();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		QuitComponent build();
	}

}
