package org.cryptomator.ui.traymenu;

import javafx.beans.Observable;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
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
			// TODO: schedule async to not delay tray menu initialization
			showMainWindow(null);
		}
	}

	private void vaultListChanged(Observable observable) {
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
			// TODO what do we want to do with these? lock/unlock? reveal? submenu?
			MenuItem vaultItem = new MenuItem(v.getDisplayableName());
			menu.add(vaultItem);
		}
		menu.addSeparator();

		MenuItem quitApplicationItem = new MenuItem("TODO quit");
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);

	}

	private void showMainWindow(ActionEvent actionEvent) {
		fxApplicationStarter.get(true).showMainWindow();
	}

	private void showPreferencesWindow(EventObject actionEvent) {
		fxApplicationStarter.get(true).showPreferencesWindow();
	}

	private void quitApplication(ActionEvent actionEvent) {
		shutdownLatch.countDown();
	}
}
