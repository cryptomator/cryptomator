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
import javafx.scene.image.Image;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainModule;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.unlock.UnlockComponent;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.ResourceBundle;

@Module(includes = {KeychainModule.class, UpdateCheckerModule.class}, subcomponents = {MainWindowComponent.class, PreferencesComponent.class, UnlockComponent.class, QuitComponent.class})
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
	@Named("windowIcon")
	@FxApplicationScoped
	static Optional<Image> provideWindowIcon() {
		if (SystemUtils.IS_OS_MAC) {
			return Optional.empty();
		}
		try (InputStream in = FxApplicationModule.class.getResourceAsStream("/window_icon_32.png")) { // TODO: use some higher res depending on display?
			return Optional.of(new Image(in));
		} catch (IOException e) {
			return Optional.empty();
		}
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
