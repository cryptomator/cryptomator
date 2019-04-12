/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import dagger.BindsInstance;
import dagger.Subcomponent;
import javafx.application.Application;
import javafx.stage.Stage;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.controllers.ViewControllerLoader;

import javax.inject.Named;

@FxApplicationScoped
@Subcomponent(modules = FxApplicationModule.class)
interface FxApplicationComponent {

	ViewControllerLoader fxmlLoader();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder fxApplication(Application application);

		@BindsInstance
		Builder mainWindow(@Named("mainWindow") Stage mainWindow);

		FxApplicationComponent build();

	}

}
