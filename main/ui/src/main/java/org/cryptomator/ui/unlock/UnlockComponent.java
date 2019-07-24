/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.unlock;

import dagger.BindsInstance;
import dagger.Subcomponent;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.model.Vault;

@UnlockScoped
@Subcomponent(modules = {UnlockModule.class})
public interface UnlockComponent {

	@UnlockWindow
	Stage window();

	@UnlockWindow
	FXMLLoaderFactory fxmlLoaders();

	default void showUnlockWindow() {
		Stage stage = window();
		fxmlLoaders().setScene("/fxml/unlock2.fxml", stage); // TODO rename fxml file
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {
		
		@BindsInstance
		Builder vault(@UnlockWindow Vault vault);

		UnlockComponent build();
	}

}
