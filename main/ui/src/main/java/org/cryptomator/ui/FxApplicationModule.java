/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui;

import dagger.Binds;
import dagger.Module;
import javafx.application.Application;

@Module(includes = {UiModule.class})
abstract class FxApplicationModule {

	@Binds
	@FxApplicationScoped
	abstract Application provideApplication(FxApplication application);

}
