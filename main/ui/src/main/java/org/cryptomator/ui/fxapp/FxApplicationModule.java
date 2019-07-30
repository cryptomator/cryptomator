/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.fxapp;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.preferences.PreferencesComponent;

import java.util.ResourceBundle;

@Module(includes = {KeychainModule.class}, subcomponents = {MainWindowComponent.class, PreferencesComponent.class})
abstract class FxApplicationModule {

	@Binds
	@FxApplicationScoped
	abstract Application provideApplication(FxApplication application);

	@Provides
	@FxApplicationScoped
	static ObjectProperty<Vault> provideSelectedVault() {
		return new SimpleObjectProperty<>();
	}
	
	@Provides
	@FxApplicationScoped
	static ResourceBundle provideLocalization() {
		return ResourceBundle.getBundle("i18n.strings");
	}
	
	@Provides
	static MainWindowComponent provideMainWindowComponent(MainWindowComponent.Builder builder) {
		return builder.build();
	}

	@Provides
	static PreferencesComponent providePreferencesComponent(PreferencesComponent.Builder builder) {
		return builder.build();
	}

}
