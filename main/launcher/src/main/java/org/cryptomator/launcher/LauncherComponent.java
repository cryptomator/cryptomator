package org.cryptomator.launcher;

import javax.inject.Singleton;

import org.cryptomator.logging.DebugMode;
import org.cryptomator.ui.controllers.MainController;

import dagger.Component;

@Singleton
@Component(modules = LauncherModule.class)
interface LauncherComponent {

	MainController mainController();

	DebugMode debugMode();

}
