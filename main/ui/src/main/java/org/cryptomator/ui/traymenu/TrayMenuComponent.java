/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.traymenu;

import dagger.Subcomponent;

import java.awt.SystemTray;

@TrayMenuScoped
@Subcomponent
public interface TrayMenuComponent {

	TrayIconController trayIconController();

	/**
	 * @return <code>true</code> if a tray icon can be installed
	 */
	default boolean isSupported() {
		return SystemTray.isSupported();
	}

	/**
	 * @return <code>true</code> if a tray icon has been installed
	 */
	default boolean isInitialized() {
		return trayIconController().isInitialized();
	}

	/**
	 * Installs a tray icon to the system tray.
	 *
	 * @throws IllegalStateException If already added
	 */
	default void addIconToSystemTray() throws IllegalStateException {
		assert isSupported();
		trayIconController().initializeTrayIcon();
	}

	@Subcomponent.Builder
	interface Builder {

		TrayMenuComponent build();
	}

}
