package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.integrations.tray.ActionItem;
import org.cryptomator.integrations.tray.SeparatorItem;
import org.cryptomator.integrations.tray.SubMenuItem;
import org.cryptomator.integrations.tray.TrayIconLoader;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.integrations.tray.TrayMenuException;
import org.cryptomator.integrations.tray.TrayMenuItem;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationTerminator;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@TrayMenuScoped
public class TrayMenuBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(TrayMenuBuilder.class);
	private static final String TRAY_ICON_MAC = "/img/tray_icon_mac@2x.png";
	private static final String TRAY_ICON_UNLOCKED_MAC = "/img/tray_icon_unlocked_mac@2x.png";
	private static final String TRAY_ICON = "/img/tray_icon.png";
	private static final String TRAY_ICON_UNLOCKED = "/img/tray_icon_unlocked.png";

	private final ResourceBundle resourceBundle;
	private final VaultService vaultService;
	private final FxApplicationWindows appWindows;
	private final FxApplicationTerminator appTerminator;
	private final ObservableList<Vault> vaults;
	private final TrayMenuController trayMenu;

	private volatile boolean initialized;

	@Inject
	TrayMenuBuilder(ResourceBundle resourceBundle, VaultService vaultService, FxApplicationWindows appWindows, FxApplicationTerminator appTerminator, ObservableList<Vault> vaults, Optional<TrayMenuController> trayMenu) {
		this.resourceBundle = resourceBundle;
		this.vaultService = vaultService;
		this.appWindows = appWindows;
		this.appTerminator = appTerminator;
		this.vaults = vaults;
		this.trayMenu = trayMenu.orElse(null);
	}

	public synchronized void initTrayMenu() {
		Preconditions.checkState(!initialized, "tray icon already initialized");

		vaults.addListener(this::vaultListChanged);
		vaults.forEach(v -> {
			v.displayNameProperty().addListener(this::vaultListChanged);
		});

		try {
			trayMenu.showTrayIcon(loader -> {
				switch (loader) {
					case TrayIconLoader.PngData l -> l.loadPng(getAppropriateTrayIconImage());
					case TrayIconLoader.FreedesktopIconName l -> l.lookupByName(getAppropriateFreedesktopIconName());
				}
			}, this::showMainWindow, "Cryptomator");
			trayMenu.onBeforeOpenMenu(() -> {
				for (Vault vault : vaults) {
					VaultListManager.redetermineVaultState(vault);
				}
			});
			rebuildMenu();
			initialized = true;
		} catch (TrayMenuException e) {
			LOG.error("Adding tray icon failed", e);
		}
	}

	public boolean isInitialized() {
		return initialized;
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		trayMenu.updateTrayIcon(loader -> {
			switch (loader) {
				case TrayIconLoader.PngData l -> l.loadPng(getAppropriateTrayIconImage());
				case TrayIconLoader.FreedesktopIconName l -> l.lookupByName(getAppropriateFreedesktopIconName());
			}
		});
		rebuildMenu();
	}

	private void rebuildMenu() {
		List<TrayMenuItem> menu = new ArrayList<>();

		menu.add(new ActionItem(resourceBundle.getString("traymenu.showMainWindow"), this::showMainWindow));
		menu.add(new ActionItem(resourceBundle.getString("traymenu.showPreferencesWindow"), this::showPreferencesWindow));
		menu.add(new SeparatorItem());
		for (Vault vault : vaults) {
			List<TrayMenuItem> submenu = buildSubmenu(vault);
			var label = vault.isUnlocked() ? "* ".concat(vault.getDisplayName()) : vault.getDisplayName();
			menu.add(new SubMenuItem(label, submenu));
		}
		menu.add(new SeparatorItem());
		menu.add(new ActionItem(resourceBundle.getString("traymenu.lockAllVaults"), this::lockAllVaults, vaults.stream().anyMatch(Vault::isUnlocked)));
		menu.add(new ActionItem(resourceBundle.getString("traymenu.quitApplication"), this::quitApplication));

		try {
			trayMenu.updateTrayMenu(menu);
		} catch (TrayMenuException e) {
			LOG.error("Updating tray menu failed", e);
		}
	}

	private List<TrayMenuItem> buildSubmenu(Vault vault) {
		if (vault.isLocked()) {
			return List.of( //
					new ActionItem(resourceBundle.getString("traymenu.vault.unlock"), () -> this.unlockVault(vault)) //
			);
		} else if (vault.isUnlocked()) {
			return List.of( //
					new ActionItem(resourceBundle.getString("traymenu.vault.lock"), () -> this.lockVault(vault)), //
					new ActionItem(resourceBundle.getString("traymenu.vault.reveal"), () -> this.revealVault(vault)) //
			);
		} else {
			return List.of();
		}
	}

	/* action listeners: */

	private void quitApplication() {
		appTerminator.terminate();
	}

	private void unlockVault(Vault vault) {
		appWindows.startUnlockWorkflow(vault, null);
	}

	private void lockVault(Vault vault) {
		appWindows.startLockWorkflow(vault, null);
	}

	private void lockAllVaults() {
		vaultService.lockAll(vaults.filtered(Vault::isUnlocked), false);
	}

	private void revealVault(Vault vault) {
		vaultService.reveal(vault);
	}

	void showMainWindow() {
		appWindows.showMainWindow();
	}

	private void showPreferencesWindow() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.ANY);
	}

	private byte[] getAppropriateTrayIconImage() {
		boolean isAnyVaultUnlocked = vaults.stream().anyMatch(Vault::isUnlocked);

		String resourceName;
		if (SystemUtils.IS_OS_MAC_OSX) {
			resourceName = isAnyVaultUnlocked ? TRAY_ICON_UNLOCKED_MAC : TRAY_ICON_MAC;
		} else {
			resourceName = isAnyVaultUnlocked ? TRAY_ICON_UNLOCKED : TRAY_ICON;
		}

		try (var image = getClass().getResourceAsStream(resourceName)) {
			assert image != null;
			return image.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load tray icon image: " + resourceName, e);
		}
	}

	private String getAppropriateFreedesktopIconName() {
		boolean isAnyVaultUnlocked = vaults.stream().anyMatch(Vault::isUnlocked);
		return isAnyVaultUnlocked ? "org.cryptomator.Cryptomator.tray-unlocked-symbolic" : "org.cryptomator.Cryptomator.tray-symbolic";
	}
}
