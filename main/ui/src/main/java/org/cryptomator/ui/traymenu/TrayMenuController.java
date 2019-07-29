package org.cryptomator.ui.traymenu;

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
	private final PopupMenu menu;

	@Inject
	TrayMenuController(FxApplicationStarter fxApplicationStarter, @Named("shutdownLatch") CountDownLatch shutdownLatch) {
		this.fxApplicationStarter = fxApplicationStarter;
		this.shutdownLatch = shutdownLatch;
		this.menu = new PopupMenu();
	}

	public PopupMenu getMenu() {
		return menu;
	}

	public void initTrayMenu() {
		// TODO add listeners
		rebuildMenu();
		
		// register preferences shortcut
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::showPreferencesWindow);
		}
	}

	private void rebuildMenu() {
		MenuItem showMainWindowItem = new MenuItem("TODO show");
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);

		MenuItem showPreferencesItem = new MenuItem("TODO preferences");
		showPreferencesItem.addActionListener(this::showPreferencesWindow);
		menu.add(showPreferencesItem);

		menu.addSeparator();
		// foreach vault: add submenu
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
