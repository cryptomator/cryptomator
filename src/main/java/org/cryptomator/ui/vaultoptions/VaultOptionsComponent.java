/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.vaultoptions;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

@VaultOptionsScoped
@Subcomponent(modules = {VaultOptionsModule.class})
public interface VaultOptionsComponent {

	@VaultOptionsWindow
	Stage window();

	@FxmlScene(FxmlFile.VAULT_OPTIONS)
	Lazy<Scene> scene();

	ObjectProperty<SelectedVaultOptionsTab> selectedTabProperty();

	default Stage showVaultOptionsWindow(SelectedVaultOptionsTab selectedTab) {
		selectedTabProperty().set(selectedTab);
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		stage.requestFocus();
		return stage;
	}

	@Subcomponent.Factory
	interface Factory {

		VaultOptionsComponent create(@BindsInstance @VaultOptionsWindow Vault vault);
	}

}
