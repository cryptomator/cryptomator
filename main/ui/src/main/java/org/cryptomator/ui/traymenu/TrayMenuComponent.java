/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.traymenu;

import dagger.Subcomponent;

import java.awt.SystemTray;

@TrayMenuScoped
@Subcomponent(modules = {TrayMenuModule.class})
public interface TrayMenuComponent {

	TrayIconController trayIconController();

	default void addIconToSystemTray() {
		if (SystemTray.isSupported()) {
			trayIconController().initializeTrayIcon();
		} else {
			// TODO what?
		}
	}

	@Subcomponent.Builder
	interface Builder {

		TrayMenuComponent build();
	}

}
