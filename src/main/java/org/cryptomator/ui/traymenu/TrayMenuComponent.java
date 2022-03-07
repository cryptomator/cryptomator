/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import dagger.Subcomponent;
import org.cryptomator.integrations.tray.TrayMenuController;

import java.awt.SystemTray;
import java.util.Optional;

@TrayMenuScoped
@Subcomponent(modules = {TrayMenuModule.class})
public interface TrayMenuComponent {

	Optional<TrayMenuController> trayMenuController();

	TrayMenuBuilder trayMenuBuilder();

	/**
	 * @return <code>true</code> if a tray icon can be installed
	 */
	default boolean isSupported() {
		// TODO add isSupported to API and move SystemTray.isSupported() to impl
		return trayMenuController().isPresent() && SystemTray.isSupported();
	}

	/**
	 * @return <code>true</code> if a tray icon has been installed
	 */
	default boolean isInitialized() {
		return isSupported() && trayMenuBuilder().isInitialized();
	}

	/**
	 * Installs a tray icon to the system tray.
	 *
	 * @throws IllegalStateException If already added
	 */
	default void initializeTrayIcon() throws IllegalStateException {
		Preconditions.checkState(isSupported(), "system tray not supported");
		trayMenuBuilder().initTrayMenu();
	}

	@Subcomponent.Builder
	interface Builder {

		TrayMenuComponent build();
	}

}
