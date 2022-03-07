package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.tray.ActionItem;
import org.cryptomator.integrations.tray.SeparatorItem;
import org.cryptomator.integrations.tray.SubMenuItem;
import org.cryptomator.integrations.tray.TrayMenuItem;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.launcher.AppLifecycleListener;
import org.cryptomator.ui.launcher.FxApplicationStarter;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@TrayMenuScoped
public class TrayMenuController {

	private static final String TRAY_ICON_MAC = "/img/tray_icon_mac.png";
	private static final String TRAY_ICON = "/img/tray_icon.png";

	private final ResourceBundle resourceBundle;
	private final AppLifecycleListener appLifecycle;
	private final FxApplicationStarter fxApplicationStarter;
	private final ObservableList<Vault> vaults;
	private final org.cryptomator.integrations.tray.TrayMenuController trayMenu;

	private volatile boolean initialized;

	@Inject
	TrayMenuController(ResourceBundle resourceBundle, AppLifecycleListener appLifecycle, FxApplicationStarter fxApplicationStarter, ObservableList<Vault> vaults, Optional<org.cryptomator.integrations.tray.TrayMenuController> trayMenu) {
		this.resourceBundle = resourceBundle;
		this.appLifecycle = appLifecycle;
		this.fxApplicationStarter = fxApplicationStarter;
		this.vaults = vaults;
		this.trayMenu = trayMenu.orElse(null);
	}

	public synchronized void initTrayMenu() {
		Preconditions.checkState(!initialized, "tray icon already initialized");

		vaults.addListener(this::vaultListChanged);
		vaults.forEach(v -> {
			v.displayNameProperty().addListener(this::vaultListChanged);
		});
		rebuildMenu();

		try (var image = getClass().getResourceAsStream(SystemUtils.IS_OS_MAC_OSX ? TRAY_ICON_MAC : TRAY_ICON)) {
			trayMenu.showTrayIcon(image, this::showMainWindow, "Cryptomator");
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load embedded resource", e);
		}

		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		rebuildMenu();
	}

	private void rebuildMenu() {
		List<TrayMenuItem> menu = new ArrayList<>();

		menu.add(new ActionItem(resourceBundle.getString("traymenu.showMainWindow"), this::showMainWindow));
		menu.add(new ActionItem(resourceBundle.getString("traymenu.showPreferencesWindow"), this::showPreferencesWindow));
		menu.add(new SeparatorItem());
		for (Vault vault : vaults) {
			List<TrayMenuItem> submenu = buildSubmenu(vault);
			menu.add(new SubMenuItem(vault.getDisplayName(), submenu));
		}
		menu.add(new SeparatorItem());
		menu.add(new ActionItem(resourceBundle.getString("traymenu.lockAllVaults"), this::lockAllVaults));
		menu.add(new ActionItem(resourceBundle.getString("traymenu.quitApplication"), this::quitApplication));
// 		lockAllItem.setEnabled(!vaults.filtered(Vault::isUnlocked).isEmpty());

		trayMenu.updateTrayMenu(menu);
	}

	private List<TrayMenuItem> buildSubmenu(Vault vault) {
		if (vault.isLocked()) {
			return List.of(
					new ActionItem(resourceBundle.getString("traymenu.vault.unlock"), () -> this.unlockVault(vault))
			);
		} else if (vault.isUnlocked()) {
			return List.of(
					new ActionItem(resourceBundle.getString("traymenu.vault.lock"), () -> this.lockVault(vault)),
					new ActionItem(resourceBundle.getString("traymenu.vault.reveal"), () -> this.revealVault(vault))

			);
		} else {
			return List.of();
		}
	}

	/* action listeners: */

	private void quitApplication() {
		appLifecycle.quit();
	}

	private void unlockVault(Vault vault) {
		showMainAppAndThen(app -> app.startUnlockWorkflow(vault, Optional.empty()));
	}

	private void lockVault(Vault vault) {
		showMainAppAndThen(app -> app.startLockWorkflow(vault, Optional.empty()));
	}

	private void lockAllVaults() {
		showMainAppAndThen(app -> app.getVaultService().lockAll(vaults.filtered(Vault::isUnlocked), false));
	}

	private void revealVault(Vault vault) {
		showMainAppAndThen(app -> app.getVaultService().reveal(vault));
	}

	void showMainWindow() {
		showMainAppAndThen(FxApplication::showMainWindow);
	}

	private void showPreferencesWindow() {
		showMainAppAndThen(app -> app.showPreferencesWindow(SelectedPreferencesTab.ANY));
	}

	private void showMainAppAndThen(Consumer<FxApplication> action) {
		fxApplicationStarter.get().thenAccept(action);
	}

}
