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

import javafx.scene.Scene;
import javafx.stage.Stage;

@VaultOptionsScoped
@Subcomponent(modules = {VaultOptionsModule.class})
public interface VaultOptionsComponent {

	@VaultOptionsWindow
	Stage window();

	@FxmlScene(FxmlFile.VAULT_OPTIONS)
	Lazy<Scene> scene();

	default void showVaultOptionsWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		stage.requestFocus();
	}

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vault(@VaultOptionsWindow Vault vault);

		VaultOptionsComponent build();
	}

}
