/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.stage.Stage;

@Singleton
class ExitUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ExitUtil.class);

	private final Stage mainWindow;
	private final Localization localization;
	private final Settings settings;

	@Inject
	public ExitUtil(@Named("mainWindow") Stage mainWindow, Localization localization, Settings settings) {
		this.mainWindow = mainWindow;
		this.localization = localization;
		this.settings = settings;
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
		final TrayIcon trayIcon = createTrayIcon(exitCommand);
		try {
			SystemTray.getSystemTray().add(trayIcon);
			mainWindow.setOnCloseRequest((e) -> {
				if (Platform.isImplicitExit()) {
					exitCommand.run();
				} else {
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

		final Image image;
		if (SystemUtils.IS_OS_MAC_OSX && isMacMenuBarDarkMode()) {
			image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon_mac_white.png"));
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon_mac_black.png"));
		} else {
			image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon.png"));
		}

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
		if (settings.getNumTrayNotifications() <= 0) {
			return;
		} else {
			settings.setNumTrayNotifications(settings.getNumTrayNotifications() - 1);
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

	private void restoreFromTray(ActionEvent event) {
		Platform.runLater(() -> {
			mainWindow.show();
			mainWindow.requestFocus();
		});
	}

}
