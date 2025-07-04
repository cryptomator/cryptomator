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
import java.util.ResourceBundle;

@AddVaultWizardScoped
@Subcomponent(modules = {AddVaultModule.class})
public interface AddVaultWizardComponent {

	@AddVaultWizardWindow
	Stage window();

	@FxmlScene(FxmlFile.ADDVAULT_NEW_NAME)
	Lazy<Scene> sceneNew();

	@FxmlScene(FxmlFile.ADDVAULT_EXISTING)
	Lazy<Scene> sceneExisting();

	default void showAddNewVaultWizard(ResourceBundle resourceBundle) {
		Stage stage = window();
		stage.setScene(sceneNew().get());
		stage.setTitle(resourceBundle.getString("addvaultwizard.new.title"));
		stage.sizeToScene();
		stage.show();
	}

	default void showAddExistingVaultWizard(ResourceBundle resourceBundle) {
		Stage stage = window();
		stage.setScene(sceneExisting().get());
		stage.setTitle(resourceBundle.getString("addvaultwizard.existing.title"));
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		AddVaultWizardComponent build();
	}

}
