package org.cryptomator.ui.traymenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;

@TrayMenuScoped
public class TrayMenuController {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrayMenuController.class);

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
	}
	
	private void rebuildMenu() {
		MenuItem showMainWindowItem = new MenuItem("TODO show");
		showMainWindowItem.addActionListener(this::showMainWindow);
		menu.add(showMainWindowItem);
		
		menu.addSeparator();
		// foreach vault: add submenu
		menu.addSeparator();
		
		MenuItem quitApplicationItem = new MenuItem("TODO quit");
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);
		
	}

	private void showMainWindow(ActionEvent actionEvent) {
		fxApplicationStarter.get().showMainWindow();
	}

	private void quitApplication(ActionEvent actionEvent) {
		shutdownLatch.countDown();
	}
}
