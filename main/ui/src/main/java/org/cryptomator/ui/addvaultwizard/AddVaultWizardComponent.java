/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.scene.Scene;
import javafx.stage.Stage;

@AddVaultWizardScoped
@Subcomponent(modules = {AddVaultModule.class})
public interface AddVaultWizardComponent {

	@AddVaultWizardWindow
	Stage window();

	@FxmlScene(FxmlFile.ADDVAULT_WELCOME)
	Lazy<Scene> scene();

	default void showAddVaultWizard() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		AddVaultWizardComponent build();
	}

}
