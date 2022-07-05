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

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.desktop.QuitResponse;
import java.util.concurrent.atomic.AtomicReference;

@QuitScoped
@Subcomponent(modules = {QuitModule.class})
public interface QuitComponent {

	@QuitWindow
	Stage window();

	@FxmlScene(FxmlFile.QUIT)
	Lazy<Scene> quitScene();

	@FxmlScene(FxmlFile.QUIT_FORCED)
	Lazy<Scene> quitForcedScene();

	@QuitWindow
	AtomicReference<QuitResponse> quitResponse();

	default void showQuitWindow(QuitResponse response, boolean forced) {
		Stage stage = window();
		quitResponse().set(response);
		if(forced){
			stage.setScene(quitForcedScene().get());
		} else{
			stage.setScene(quitScene().get());
		}
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {
		QuitComponent build();
	}
}