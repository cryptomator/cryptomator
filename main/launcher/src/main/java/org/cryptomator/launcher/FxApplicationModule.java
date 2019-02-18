/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.UiModule;

import javax.inject.Named;
import java.util.function.Consumer;

@Module(includes = {UiModule.class})
class FxApplicationModule {

	@Provides
	@FxApplicationScoped
	@Named("shutdownTaskScheduler")
	Consumer<Runnable> provideShutdownTaskScheduler() {
		return CleanShutdownPerformer::scheduleShutdownTask;
	}

}
