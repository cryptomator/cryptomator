/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.unlock;

import dagger.BindsInstance;
import dagger.Lazy;
import dagger.Subcomponent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.common.vaults.Vault;

@UnlockScoped
@Subcomponent(modules = {UnlockModule.class})
public interface UnlockComponent {

	@UnlockWindow
	Stage window();

	@FxmlScene(FxmlFile.UNLOCK)
	Lazy<Scene> scene();

	default Stage showUnlockWindow() {
		Stage stage = window();
		stage.setScene(scene().get());
		stage.show();
		return stage;
	}

	@Subcomponent.Builder
	interface Builder {
		
		@BindsInstance
		Builder vault(@UnlockWindow Vault vault);

		UnlockComponent build();
	}

}
