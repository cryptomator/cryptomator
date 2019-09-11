package org.cryptomator.ui.launcher;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.traymenu.TrayMenuComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.SystemTray;

@Singleton
public class UiLauncher {

	private final Settings settings;
	private final TrayMenuComponent.Builder trayComponent;
	private final FxApplicationStarter fxApplicationStarter;
	private final AppLaunchEventHandler launchEventHandler;

	@Inject
	public UiLauncher(Settings settings, TrayMenuComponent.Builder trayComponent, FxApplicationStarter fxApplicationStarter, AppLaunchEventHandler launchEventHandler) {
		this.settings = settings;
		this.trayComponent = trayComponent;
		this.fxApplicationStarter = fxApplicationStarter;
		this.launchEventHandler = launchEventHandler;
	}

	public void launch() {
		boolean hasTrayIcon = false;
		if (SystemTray.isSupported()) {
			trayComponent.build().addIconToSystemTray();
			hasTrayIcon = true;
		}

		// show window on start?
		if (!settings.startHidden().get()) {
			fxApplicationStarter.get(hasTrayIcon).thenAccept(FxApplication::showMainWindow);
		}

		launchEventHandler.startHandlingLaunchEvents(hasTrayIcon);
	}

}
