package org.cryptomator.ui.traymenu;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.fxapp.FxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.desktop.QuitResponse;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@TrayMenuScoped
class TrayMenuController {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrayMenuController.class);

	private final ResourceBundle resourceBundle;
	private final FxApplicationStarter fxApplicationStarter;
	private final CountDownLatch shutdownLatch;
	private final Settings settings;
	private final ObservableList<Vault> vaults;
	private final PopupMenu menu;
	private final AtomicBoolean allVaultsAreLocked;

	@Inject
	TrayMenuController(ResourceBundle resourceBundle, FxApplicationStarter fxApplicationStarter, @Named("shutdownLatch") CountDownLatch shutdownLatch, Settings settings, ObservableList<Vault> vaults) {
		this.resourceBundle = resourceBundle;
		this.fxApplicationStarter = fxApplicationStarter;
		this.shutdownLatch = shutdownLatch;
		this.settings = settings;
		this.vaults = vaults;
		this.menu = new PopupMenu();
		this.allVaultsAreLocked = new AtomicBoolean(true);
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

		// register quit handler
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
			Desktop.getDesktop().setQuitHandler(this::handleQuitRequest);
		}

		// show window on start?
		if (!settings.startHidden().get()) {
			showMainWindow(null);
		}
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		rebuildMenu();
		boolean allLocked = vaults.stream().allMatch(Vault::isLocked);
		// TODO remove logging
		LOG.warn("allLocked: {}", allLocked);
		allVaultsAreLocked.set(allLocked);
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

		MenuItem quitApplicationItem = new MenuItem(resourceBundle.getString("traymenu.quitApplication"));
		quitApplicationItem.addActionListener(this::quitApplication);
		menu.add(quitApplicationItem);
	}

	private Menu buildSubmenu(Vault vault) {
		Menu submenu = new Menu(vault.getDisplayableName());

		// TODO add action listeners
		if (vault.isLocked()) {
			MenuItem unlockItem = new MenuItem(resourceBundle.getString("traymenu.vault.unlock"));
			unlockItem.addActionListener(createActionListenerForVault(vault, this::unlockVault));
			submenu.add(unlockItem);
		} else if (vault.isUnlocked()) {
			MenuItem lockItem = new MenuItem(resourceBundle.getString("traymenu.vault.lock"));
			submenu.add(lockItem);

			MenuItem revealItem = new MenuItem(resourceBundle.getString("traymenu.vault.reveal"));
			submenu.add(revealItem);
		}

		return submenu;
	}

	private ActionListener createActionListenerForVault(Vault vault, Consumer<Vault> consumer) {
		return actionEvent -> consumer.accept(vault);
	}

	private void unlockVault(Vault vault) {
		fxApplicationStarter.get(true).thenAccept(app -> app.showUnlockWindow(vault));
	}

	void showMainWindow(@SuppressWarnings("unused") ActionEvent actionEvent) {
		fxApplicationStarter.get(true).thenAccept(app -> app.showMainWindow());
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		fxApplicationStarter.get(true).thenAccept(FxApplication::showPreferencesWindow);
	}

	private void handleQuitRequest(EventObject e, QuitResponse response) {
		if (allVaultsAreLocked.get()) {
			response.performQuit(); // really?
		} else {
			fxApplicationStarter.get(true).thenAccept(app -> app.showQuitWindow(response));
		}
	}

	private void quitApplication(EventObject actionEvent) {
		handleQuitRequest(actionEvent, new QuitResponse() {
			@Override
			public void performQuit() {
				shutdownLatch.countDown();
			}

			@Override
			public void cancelQuit() {
				// no-op
			}
		});
	}
}
