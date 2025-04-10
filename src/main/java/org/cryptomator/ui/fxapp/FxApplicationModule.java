/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.fxapp;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.ui.decryptname.DecryptNameComponent;
import org.cryptomator.ui.error.ErrorComponent;
import org.cryptomator.ui.eventview.EventViewComponent;
import org.cryptomator.ui.health.HealthCheckComponent;
import org.cryptomator.ui.lock.LockComponent;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.sharevault.ShareVaultComponent;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.cryptomator.ui.updatereminder.UpdateReminderComponent;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Named;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import java.io.IOException;
import java.io.InputStream;

@Module(includes = {UpdateCheckerModule.class}, subcomponents = {TrayMenuComponent.class, //
		DecryptNameComponent.class, //
		MainWindowComponent.class, //
		PreferencesComponent.class, //
		VaultOptionsComponent.class, //
		UnlockComponent.class, //
		LockComponent.class, //
		QuitComponent.class, //
		ErrorComponent.class, //
		HealthCheckComponent.class, //
		UpdateReminderComponent.class, //
		ShareVaultComponent.class, //
		EventViewComponent.class})
abstract class FxApplicationModule {

	private static Image createImageFromResource(String resourceName) throws IOException {
		try (InputStream in = FxApplicationModule.class.getResourceAsStream(resourceName)) {
			return new Image(in);
		}
	}

	@Provides
	@FxApplicationScoped
	static TrayMenuComponent provideTrayMenuComponent(TrayMenuComponent.Builder builder) {
		return builder.build();
	}

	@Provides
	@FxApplicationScoped
	static MainWindowComponent provideMainWindowComponent(MainWindowComponent.Builder builder) {
		return builder.build();
	}

	@Provides
	@FxApplicationScoped
	static PreferencesComponent providePreferencesComponent(PreferencesComponent.Builder builder) {
		return builder.build();
	}

	@Provides
	@FxApplicationScoped
	static QuitComponent provideQuitComponent(QuitComponent.Builder builder) {
		return builder.build();
	}

	@Provides
	@FxApplicationScoped
	static EventViewComponent provideEventViewComponent(EventViewComponent.Factory factory) {
		return factory.create();
	}

}