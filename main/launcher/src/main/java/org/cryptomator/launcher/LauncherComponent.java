/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import javax.inject.Singleton;

import org.cryptomator.logging.DebugMode;
import org.cryptomator.ui.controllers.ViewControllerLoader;

import dagger.Component;

@Singleton
@Component(modules = LauncherModule.class)
interface LauncherComponent {

	ViewControllerLoader fxmlLoader();

	DebugMode debugMode();

}
