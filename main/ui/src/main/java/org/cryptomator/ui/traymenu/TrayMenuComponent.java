/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.traymenu;

import dagger.Subcomponent;
import org.cryptomator.ui.fxapp.FxApplication;

import java.awt.SystemTray;

@TrayMenuScoped
@Subcomponent(modules = {TrayMenuModule.class})
public interface TrayMenuComponent {

	TrayIconController trayIconController();

	FxApplicationStarter fxAppStarter();

	default void addIconToSystemTray() {
		if (SystemTray.isSupported()) {
			trayIconController().initializeTrayIcon();
		} else {
			// show main window directly without any tray support:
			fxAppStarter().get(false).thenAccept(FxApplication::showMainWindow);
		}
	}

	@Subcomponent.Builder
	interface Builder {

		TrayMenuComponent build();
	}

}
