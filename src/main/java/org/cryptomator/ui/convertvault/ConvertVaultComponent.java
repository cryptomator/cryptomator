/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.convertvault;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ConvertVaultScoped
@Subcomponent(modules = {ConvertVaultModule.class})
public interface ConvertVaultComponent {

	@ConvertVaultWindow
	Stage window();

	@FxmlScene(FxmlFile.CONVERTVAULT_HUBTOLOCAL)
	Lazy<Scene> hubToLocalScene();

	default void showHubToLocalWindow() {
		Stage stage = window();
		stage.setScene(hubToLocalScene().get());
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Factory
	interface Factory {

		ConvertVaultComponent create(@BindsInstance @ConvertVaultWindow Vault vault, @BindsInstance @Named("convertVaultOwner") Stage owner);
	}
}