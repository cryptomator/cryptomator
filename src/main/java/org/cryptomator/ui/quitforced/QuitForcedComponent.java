/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.quitforced;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.desktop.QuitResponse;

@QuitForcedScoped
@Subcomponent(modules = {QuitForcedModule.class})
public interface QuitForcedComponent {

	@QuitForcedWindow
	Stage window();

	@FxmlScene(FxmlFile.QUIT_FORCED)
	Lazy<Scene> scene();

	QuitForcedController controller();

	default Stage showQuitForcedWindow(QuitResponse response) {
		controller().updateQuitRequest(response);
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		stage.requestFocus();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {

		QuitForcedComponent build();
	}

}
