/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.cryptomator.ui.UiModule;

import dagger.Module;
import dagger.Provides;
import javafx.application.Application;
import javafx.stage.Stage;

@Module(includes = {UiModule.class})
class LauncherModule {

	private final Application application;
	private final Stage mainWindow;

	public LauncherModule(Application application, Stage mainWindow) {
		this.application = application;
		this.mainWindow = mainWindow;
	}

	@Provides
	@Singleton
	Application provideApplication() {
		return application;
	}

	@Provides
	@Singleton
	@Named("applicationVersion")
	Optional<String> provideApplicationVersion() {
		return ApplicationVersion.get();
	}

	@Provides
	@Singleton
	@Named("mainWindow")
	Stage provideMainWindow() {
		return mainWindow;
	}

	@Provides
	@Singleton
	@Named("fileOpenRequests")
	BlockingQueue<Path> provideFileOpenRequests() {
		return Cryptomator.FILE_OPEN_REQUESTS;
	}

	@Provides
	@Singleton
	@Named("shutdownTaskScheduler")
	Consumer<Runnable> provideShutdownTaskScheduler() {
		return CleanShutdownPerformer::scheduleShutdownTask;
	}

}
