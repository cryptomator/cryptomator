package org.cryptomator.ui.traymenu;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.launcher.AppLifecycleListener;
import org.cryptomator.ui.launcher.FxApplicationStarter;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;

import javax.inject.Inject;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@TrayMenuScoped
class TrayMenuController {

	private final ResourceBundle resourceBundle;
	private final AppLifecycleListener appLifecycle;
	private final FxApplicationStarter fxApplicationStarter;
	private final ObservableList<Vault> vaults;
	private final PopupMenu menu;

	@Inject
	TrayMenuController(ResourceBundle resourceBundle, AppLifecycleListener appLifecycle, FxApplicationStarter fxApplicationStarter, ObservableList<Vault> vaults) {
		this.resourceBundle = resourceBundle;
		this.appLifecycle = appLifecycle;
		this.fxApplicationStarter = fxApplicationStarter;
		this.vaults = vaults;
		this.menu = new PopupMenu();
	}

	public PopupMenu getMenu() {
		return menu;
	}

	public void initTrayMenu() {
		vaults.addListener(this::vaultListChanged);
		rebuildMenu();
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		rebuildMenu();
	}

	private void rebuildMenu() {
		menu.removeAll();

		MenuItem showMainWindowItem = new MenuItem(resourceBundle.getString("traymenu.showMainWindow"));
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);

		MenuItem showPreferencesItem = new MenuItem(resourceBundle.getString("traymenu.showPreferencesWindow"));
		showPreferencesItem.addActionListener(this::showPreferencesWindow);
		menu.add(showPreferencesItem);

		menu.addSeparator();
		for (Vault v : vaults) {
			MenuItem submenu = buildSubmenu(v);
			menu.add(submenu);
		}
		menu.addSeparator();

		MenuItem lockAllItem = new MenuItem(resourceBundle.getString("traymenu.lockAllVaults"));
		lockAllItem.addActionListener(this::lockAllVaults);
		lockAllItem.setEnabled(!vaults.filtered(Vault::isUnlocked).isEmpty());
		menu.add(lockAllItem);

		MenuItem quitApplicationItem = new MenuItem(resourceBundle.getString("traymenu.quitApplication"));
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);
	}

	private Menu buildSubmenu(Vault vault) {
		Menu submenu = new Menu(vault.getDisplayableName());

		if (vault.isLocked()) {
			MenuItem unlockItem = new MenuItem(resourceBundle.getString("traymenu.vault.unlock"));
			unlockItem.addActionListener(createActionListenerForVault(vault, this::unlockVault));
			submenu.add(unlockItem);
		} else if (vault.isUnlocked()) {
			MenuItem lockItem = new MenuItem(resourceBundle.getString("traymenu.vault.lock"));
			lockItem.addActionListener(createActionListenerForVault(vault, this::lockVault));
			submenu.add(lockItem);

			MenuItem revealItem = new MenuItem(resourceBundle.getString("traymenu.vault.reveal"));
			revealItem.addActionListener(createActionListenerForVault(vault, this::revealVault));
			submenu.add(revealItem);
		}

		return submenu;
	}

	private ActionListener createActionListenerForVault(Vault vault, Consumer<Vault> consumer) {
		return actionEvent -> consumer.accept(vault);
	}

	private void unlockVault(Vault vault) {
		fxApplicationStarter.get(true).thenAccept(app -> app.startUnlockWorkflow(vault, Optional.empty()));
	}

	private void lockVault(Vault vault) {
		fxApplicationStarter.get(true).thenAccept(app -> app.getVaultService().lock(vault, false));
	}

	private void lockAllVaults(ActionEvent actionEvent) {
		fxApplicationStarter.get(true).thenAccept(app -> app.getVaultService().lockAll(vaults.filtered(Vault::isUnlocked), false));
	}

	private void revealVault(Vault vault) {
		fxApplicationStarter.get(true).thenAccept(app -> app.getVaultService().reveal(vault));
	}

	void showMainWindow(@SuppressWarnings("unused") ActionEvent actionEvent) {
		fxApplicationStarter.get(true).thenAccept(app -> app.showMainWindow());
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		fxApplicationStarter.get(true).thenAccept(app -> app.showPreferencesWindow(SelectedPreferencesTab.ANY));
	}

	private void quitApplication(EventObject actionEvent) {
		appLifecycle.quit();
	}

}
