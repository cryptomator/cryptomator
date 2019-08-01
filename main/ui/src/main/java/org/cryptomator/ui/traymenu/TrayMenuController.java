package org.cryptomator.ui.traymenu;

import javafx.beans.Observable;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.concurrent.CountDownLatch;

@TrayMenuScoped
class TrayMenuController {

	private final FxApplicationStarter fxApplicationStarter;
	private final CountDownLatch shutdownLatch;
	private final Settings settings;
	private final ObservableList<Vault> vaults;
	private final PopupMenu menu;

	@Inject
	TrayMenuController(FxApplicationStarter fxApplicationStarter, @Named("shutdownLatch") CountDownLatch shutdownLatch, Settings settings, ObservableList<Vault> vaults) {
		this.fxApplicationStarter = fxApplicationStarter;
		this.shutdownLatch = shutdownLatch;
		this.settings = settings;
		this.vaults = vaults;
		this.menu = new PopupMenu();
	}

	public PopupMenu getMenu() {
		return menu;
	}

	public void initTrayMenu() {
		vaults.addListener(this::vaultListChanged);

		rebuildMenu();

		// register preferences shortcut
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::showPreferencesWindow);
		}

		// show window on start?
		if (!settings.startHidden().get()) {
			showMainWindow(null);
		}
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		rebuildMenu();
	}

	private void rebuildMenu() {
		menu.removeAll();

		MenuItem showMainWindowItem = new MenuItem("TODO show");
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);

		MenuItem showPreferencesItem = new MenuItem("TODO preferences");
		showPreferencesItem.addActionListener(this::showPreferencesWindow);
		menu.add(showPreferencesItem);

		menu.addSeparator();
		for (Vault v : vaults) {
			MenuItem submenu = buildSubmenu(v);
			menu.add(submenu);
		}
		menu.addSeparator();

		MenuItem quitApplicationItem = new MenuItem("TODO quit");
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);
	}

	private Menu buildSubmenu(Vault vault) {
		Menu submenu = new Menu(vault.getDisplayableName());
		vault.stateProperty().addListener(observable -> rebuildSubmenu(submenu, vault));
		rebuildSubmenu(submenu, vault);
		return submenu;
	}

	private void rebuildSubmenu(Menu submenu, Vault vault) {
		submenu.removeAll();

		// TODO add action listeners
		if (vault.isLocked()) {
			MenuItem unlockItem = new MenuItem("TODO unlock");
			submenu.add(unlockItem);
		} else if (vault.isUnlocked()) {
			MenuItem lockItem = new MenuItem("TODO lock");
			submenu.add(lockItem);

			MenuItem revealItem = new MenuItem("TODO reveal");
			submenu.add(revealItem);
		}
	}

	void showMainWindow(@SuppressWarnings("unused") ActionEvent actionEvent) {
		fxApplicationStarter.get(true).thenAccept(FxApplication::showMainWindow);
	}

	void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		fxApplicationStarter.get(true).thenAccept(FxApplication::showPreferencesWindow);
	}

	void quitApplication(@SuppressWarnings("unused") ActionEvent actionEvent) {
		shutdownLatch.countDown();
	}
}
