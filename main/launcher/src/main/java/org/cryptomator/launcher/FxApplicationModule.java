/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.UiModule;

import javax.inject.Named;
import java.util.function.Consumer;

@Module(includes = {UiModule.class})
abstract class FxApplicationModule {

	@Provides
	@FxApplicationScoped
	@Named("shutdownTaskScheduler")
	static Consumer<Runnable> provideShutdownTaskScheduler() {
		return CleanShutdownPerformer::scheduleShutdownTask;
	}

	@Provides
	@FxApplicationScoped
	@Named("mainWindow")
	static Stage providePrimaryStage() {
		Stage stage = new Stage();
		stage.setMinWidth(652.0);
		stage.setMinHeight(440.0);
		return stage;
	}

	@Binds
	@FxApplicationScoped
	abstract Application bindApplication(FxApplication application);


}
