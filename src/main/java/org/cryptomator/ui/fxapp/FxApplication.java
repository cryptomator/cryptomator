package org.cryptomator.ui.fxapp;

import dagger.Lazy;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@FxApplicationScoped
public class FxApplication {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final long startupTime;
	private final Environment environment;
	private final Settings settings;
	private final AppLaunchEventHandler launchEventHandler;
	private final Lazy<TrayMenuComponent> trayMenu;
	private final FxApplicationWindows appWindows;
	private final FxApplicationStyle applicationStyle;
	private final FxApplicationTerminator applicationTerminator;
	private final AutoUnlocker autoUnlocker;

	@Inject
	FxApplication(@Named("startupTime") long startupTime, Environment environment, Settings settings, AppLaunchEventHandler launchEventHandler, Lazy<TrayMenuComponent> trayMenu, FxApplicationWindows appWindows, FxApplicationStyle applicationStyle, FxApplicationTerminator applicationTerminator, AutoUnlocker autoUnlocker) {
		this.startupTime = startupTime;
		this.environment = environment;
		this.settings = settings;
		this.launchEventHandler = launchEventHandler;
		this.trayMenu = trayMenu;
		this.appWindows = appWindows;
		this.applicationStyle = applicationStyle;
		this.applicationTerminator = applicationTerminator;
		this.autoUnlocker = autoUnlocker;
	}

	//TODO: eventUpdater muss hier starten
	public void start() {
		LOG.trace("FxApplication.start()");
		applicationStyle.initialize();
		appWindows.initialize();
		applicationTerminator.initialize();

		// init system tray
		final boolean hasTrayIcon;
		if (settings.showTrayIcon.get() && trayMenu.get().isSupported()) {
			trayMenu.get().initializeTrayIcon();
			Platform.setImplicitExit(false); // don't quit when closing all windows
			hasTrayIcon = true;
		} else {
			hasTrayIcon = false;
		}

		// show main window
		appWindows.showMainWindow().thenAccept(stage -> {
			if (settings.startHidden.get()) {
				if (hasTrayIcon) {
					stage.hide();
				} else {
					stage.setIconified(true);
				}
			}
			LOG.debug("Main window initialized after {}ms", System.currentTimeMillis() - startupTime);
		}).exceptionally(error -> {
			LOG.error("Failed to show main window", error);
			return null;
		});

		var time14DaysAgo = Instant.now().minus(Duration.ofDays(14));
		if (!environment.disableUpdateCheck() //
				&& !settings.checkForUpdates.getValue() //
				&& settings.lastSuccessfulUpdateCheck.get().isBefore(time14DaysAgo) //
				&& settings.lastUpdateCheckReminder.get().isBefore(time14DaysAgo)) {
			appWindows.showUpdateReminderWindow();
		}

		migrateAndInformDokanyRemoval();

		launchEventHandler.startHandlingLaunchEvents();
		autoUnlocker.tryUnlockForTimespan(2, TimeUnit.MINUTES);
	}

	private void migrateAndInformDokanyRemoval() {
		var dokanyProviderId = "org.cryptomator.frontend.dokany.mount.DokanyMountProvider";
		boolean dokanyFound = false;
		if (settings.mountService.getValueSafe().equals(dokanyProviderId)) {
			dokanyFound = true;
			settings.mountService.set(null);
		}
		for (VaultSettings vaultSettings : settings.directories) {
			if (vaultSettings.mountService.getValueSafe().equals(dokanyProviderId)) {
				dokanyFound = true;
				vaultSettings.mountService.set(null);
			}
		}
		if (dokanyFound) {
			appWindows.showDokanySupportEndWindow();
		}
	}
}
