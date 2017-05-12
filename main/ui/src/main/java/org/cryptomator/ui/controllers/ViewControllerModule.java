/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;

@Module
public class ViewControllerModule {

	@Provides
	@IntoMap
	@ViewControllerKey(ChangePasswordController.class)
	ViewController provideChangePasswordController(ChangePasswordController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(InitializeController.class)
	ViewController provideInitializeController(InitializeController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(MainController.class)
	ViewController provideMainController(MainController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(NotFoundController.class)
	ViewController provideNotFoundController(NotFoundController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(SettingsController.class)
	ViewController provideSettingsController(SettingsController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(UnlockController.class)
	ViewController provideUnlockController(UnlockController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(UnlockedController.class)
	ViewController provideUnlockedController(UnlockedController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(UpgradeController.class)
	ViewController provideUpgradeController(UpgradeController controller) {
		return controller;
	}

	@Provides
	@IntoMap
	@ViewControllerKey(WelcomeController.class)
	ViewController provideWelcomeController(WelcomeController controller) {
		return controller;
	}

}
