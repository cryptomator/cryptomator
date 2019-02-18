/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Jean-Noël Charon - implementation of github issue #56
 *******************************************************************************/
package org.cryptomator.ui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.jni.JniException;
import org.cryptomator.jni.MacApplicationUiState;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.ui.l10n.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@FxApplicationScoped
public class ExitUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ExitUtil.class);

	private final Stage mainWindow;
	private final Localization localization;
	private final Settings settings;
	private final Optional<MacFunctions> macFunctions;
	private TrayIcon trayIcon;

	@Inject
	public ExitUtil(@Named("mainWindow") Stage mainWindow, Localization localization, Settings settings, Optional<MacFunctions> macFunctions) {
		this.mainWindow = mainWindow;
		this.localization = localization;
		this.settings = settings;
		this.macFunctions = macFunctions;
	}

	public void initExitHandler(Runnable exitCommand) {
		if (SystemUtils.IS_OS_LINUX) {
			initMinimizeExitHandler(exitCommand);
		} else {
			initTrayIconExitHandler(exitCommand);
		}
	}

	private void initMinimizeExitHandler(Runnable exitCommand) {
		mainWindow.setOnCloseRequest(e -> {
			if (Platform.isImplicitExit()) {
				exitCommand.run();
			} else {
				mainWindow.setIconified(true);
				e.consume();
			}
		});
	}

	private void initTrayIconExitHandler(Runnable exitCommand) {
		trayIcon = createTrayIcon(exitCommand);
		try {
			// double clicking tray icon should open Cryptomator
			if (SystemUtils.IS_OS_WINDOWS) {
				trayIcon.addMouseListener(new TrayIconMouseListener());
			}

			SystemTray.getSystemTray().add(trayIcon);
			mainWindow.setOnCloseRequest((e) -> {
				if (Platform.isImplicitExit()) {
					exitCommand.run();
				} else {
					macFunctions.map(MacFunctions::uiState).ifPresent(JniException.ignore(MacApplicationUiState::transformToAgentApplication));
					mainWindow.close();
					this.showTrayNotification(trayIcon);
				}
			});
		} catch (SecurityException | AWTException ex) {
			// not working? then just go ahead and close the app
			mainWindow.setOnCloseRequest((ev) -> {
				exitCommand.run();
			});
		}
	}

	private TrayIcon createTrayIcon(Runnable exitCommand) {
		final PopupMenu popup = new PopupMenu();

		final MenuItem showItem = new MenuItem(localization.getString("tray.menu.open"));
		showItem.addActionListener(this::restoreFromTray);
		popup.add(showItem);

		final MenuItem exitItem = new MenuItem(localization.getString("tray.menu.quit"));
		exitItem.addActionListener(e -> exitCommand.run());
		popup.add(exitItem);

		final Image image = getAppropriateTrayIconImage(true);

		return new TrayIcon(image, localization.getString("app.name"), popup);
	}

	/**
	 * @return true if <code>defaults read -g AppleInterfaceStyle</code> has an exit status of <code>0</code> (i.e. _not_ returning "key not found").
	 */
	private boolean isMacMenuBarDarkMode() {
		try {
			// check for exit status only. Once there are more modes than "dark" and "default", we might need to analyze string contents..
			final Process proc = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
			proc.waitFor(100, TimeUnit.MILLISECONDS);
			return proc.exitValue() == 0;
		} catch (IOException | InterruptedException | IllegalThreadStateException ex) {
			// IllegalThreadStateException thrown by proc.exitValue(), if process didn't terminate
			LOG.warn("Determining MAC OS X dark mode settings failed. Assuming default (light) mode.");
			return false;
		}
	}

	private void showTrayNotification(TrayIcon trayIcon) {
		int remainingTrayNotification = settings.numTrayNotifications().get();
		if (remainingTrayNotification <= 0) {
			return;
		} else {
			settings.numTrayNotifications().set(remainingTrayNotification - 1);
		}
		final Runnable notificationCmd;
		if (SystemUtils.IS_OS_MAC_OSX) {
			final String title = localization.getString("tray.infoMsg.title");
			final String msg = localization.getString("tray.infoMsg.msg.osx");
			final String notificationCenterAppleScript = String.format("display notification \"%s\" with title \"%s\"", msg, title);
			notificationCmd = () -> {
				try {
					final ScriptEngineManager mgr = new ScriptEngineManager();
					final ScriptEngine engine = mgr.getEngineByName("AppleScriptEngine");
					if (engine != null) {
						engine.eval(notificationCenterAppleScript);
					} else {
						Runtime.getRuntime().exec(new String[] {"/usr/bin/osascript", "-e", notificationCenterAppleScript});
					}
				} catch (ScriptException | IOException e) {
					// ignore, user will notice the tray icon anyway.
				}
			};
		} else {
			final String title = localization.getString("tray.infoMsg.title");
			final String msg = localization.getString("tray.infoMsg.msg");
			notificationCmd = () -> {
				trayIcon.displayMessage(title, msg, MessageType.INFO);
			};
		}
		SwingUtilities.invokeLater(() -> {
			notificationCmd.run();
		});
	}

	private class TrayIconMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				restoreFromTray(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
			}
		}

	}

	private void restoreFromTray(ActionEvent event) {
		Platform.runLater(() -> {
			macFunctions.map(MacFunctions::uiState).ifPresent(JniException.ignore(MacApplicationUiState::transformToForegroundApplication));
			mainWindow.show();
			mainWindow.requestFocus();
		});
	}

	public void updateTrayIcon(boolean areAllVaultsLocked) {
		if (trayIcon != null) {
			Image image = getAppropriateTrayIconImage(areAllVaultsLocked);
			trayIcon.setImage(image);
		}
	}

	private Image getAppropriateTrayIconImage(boolean areAllVaultsLocked) {
		String resourceName;
		if (SystemUtils.IS_OS_MAC_OSX && isMacMenuBarDarkMode()) {
			resourceName = areAllVaultsLocked ? "/tray_icon_mac_white.png" : "/tray_icon_unlocked_mac_white.png";
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			resourceName = areAllVaultsLocked ? "/tray_icon_mac_black.png" : "/tray_icon_unlocked_mac_black.png";
		} else {
			resourceName = areAllVaultsLocked ? "/tray_icon.png" : "/tray_icon_unlocked.png";
		}
		return Toolkit.getDefaultToolkit().getImage(getClass().getResource(resourceName));
	}

}
