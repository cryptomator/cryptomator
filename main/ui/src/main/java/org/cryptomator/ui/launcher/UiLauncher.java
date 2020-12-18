package org.cryptomator.ui.launcher;

import dagger.Lazy;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.ObservableList;
import java.awt.Desktop;
import java.awt.SystemTray;
import java.awt.desktop.AppReopenedListener;
import java.util.Collection;
import java.util.Optional;

@Singleton
public class UiLauncher {

	private static final Logger LOG = LoggerFactory.getLogger(UiLauncher.class);

	private final Settings settings;
	private final ObservableList<Vault> vaults;
	private final Lazy<TrayMenuComponent> trayMenu;
	private final FxApplicationStarter fxApplicationStarter;
	private final AppLaunchEventHandler launchEventHandler;
	private final Optional<TrayIntegrationProvider> trayIntegration;

	@Inject
	public UiLauncher(Settings settings, ObservableList<Vault> vaults, Lazy<TrayMenuComponent> trayMenu, FxApplicationStarter fxApplicationStarter, AppLaunchEventHandler launchEventHandler, Optional<TrayIntegrationProvider> trayIntegration) {
		this.settings = settings;
		this.vaults = vaults;
		this.trayMenu = trayMenu;
		this.fxApplicationStarter = fxApplicationStarter;
		this.launchEventHandler = launchEventHandler;
		this.trayIntegration = trayIntegration;
	}

	public void launch() {
		boolean hidden = settings.startHidden().get();
		if (SystemTray.isSupported() && settings.showTrayIcon().get()) {
			trayMenu.get().addIconToSystemTray();
			launch(true, hidden);
		} else {
			launch(false, hidden);
		}
	}

	private void launch(boolean withTrayIcon, boolean hidden) {
		// start hidden, minimized or normal?
		if (withTrayIcon && hidden) {
			LOG.debug("Hiding application...");
			trayIntegration.ifPresent(TrayIntegrationProvider::minimizedToTray);
		} else if (!withTrayIcon && hidden) {
			LOG.debug("Minimizing application...");
			showMainWindowAsync(true);
		} else {
			LOG.debug("Showing application...");
			showMainWindowAsync(false);
		}

		// register app reopen listener
		Desktop.getDesktop().addAppEventListener((AppReopenedListener) e -> showMainWindowAsync(false));

		// auto unlock
		Collection<Vault> vaultsToAutoUnlock = vaults.filtered(this::shouldAttemptAutoUnlock);
		if (!vaultsToAutoUnlock.isEmpty()) {
			fxApplicationStarter.get().thenAccept(app -> {
				for (Vault vault : vaultsToAutoUnlock) {
					app.startUnlockWorkflow(vault, Optional.empty());
				}
			});
		}

		launchEventHandler.startHandlingLaunchEvents();
	}

	private boolean shouldAttemptAutoUnlock(Vault vault) {
		return vault.isLocked() && vault.getVaultSettings().unlockAfterStartup().get();
	}

	private void showMainWindowAsync(boolean minimize) {
		fxApplicationStarter.get().thenCompose(FxApplication::showMainWindow).thenAccept(win -> win.setIconified(minimize));
	}

}
