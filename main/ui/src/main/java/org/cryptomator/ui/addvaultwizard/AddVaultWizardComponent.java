/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.addvaultwizard;

import dagger.Subcomponent;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;

@AddVaultWizardScoped
@Subcomponent(modules = {AddVaultModule.class})
public interface AddVaultWizardComponent {

	@AddVaultWizard
	Stage window();

	@AddVaultWizard
	FXMLLoaderFactory fxmlLoaders();

	default void showAddVaultWizard() {
		Stage stage = window();
		fxmlLoaders().setScene("/fxml/addvault_welcome.fxml", stage);
		stage.sizeToScene();
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {

		AddVaultWizardComponent build();
	}

}
