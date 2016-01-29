/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

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
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.stage.Stage;

public final class TrayIconUtil {

	private static TrayIconUtil INSTANCE;
	private static final Logger LOG = LoggerFactory.getLogger(TrayIconUtil.class);

	private final Stage mainApplicationWindow;
	private final ResourceBundle rb;
	private final Runnable exitCommand;

	/**
	 * This will add an icon to the system tray and modify the application shutdown procedure. Depending on
	 * {@link Platform#isImplicitExit()} the application may still be running, allowing shutdown using the tray menu.
	 */
	public synchronized static void init(Stage mainApplicationWindow, ResourceBundle rb, Runnable exitCommand) {
		if (INSTANCE == null && SystemTray.isSupported()) {
			INSTANCE = new TrayIconUtil(mainApplicationWindow, rb, exitCommand);
		}
	}

	private TrayIconUtil(Stage mainApplicationWindow, ResourceBundle rb, Runnable exitCommand) {
		this.mainApplicationWindow = mainApplicationWindow;
		this.rb = rb;
		this.exitCommand = exitCommand;

		initTrayIcon();
	}

	private void initTrayIcon() {
		final TrayIcon trayIcon = createTrayIcon();
		try {
			SystemTray.getSystemTray().add(trayIcon);
			mainApplicationWindow.setOnCloseRequest((e) -> {
				if (Platform.isImplicitExit()) {
					exitCommand.run();
				} else {
					mainApplicationWindow.close();
					this.showTrayNotification(trayIcon);
				}
			});
		} catch (SecurityException | AWTException ex) {
			// not working? then just go ahead and close the app
			mainApplicationWindow.setOnCloseRequest((ev) -> {
				exitCommand.run();
			});
		}
	}

	private TrayIcon createTrayIcon() {
		final PopupMenu popup = new PopupMenu();

		final MenuItem showItem = new MenuItem(rb.getString("tray.menu.open"));
		showItem.addActionListener(this::restoreFromTray);
		popup.add(showItem);

		final MenuItem exitItem = new MenuItem(rb.getString("tray.menu.quit"));
		exitItem.addActionListener(this::quitFromTray);
		popup.add(exitItem);

		final Image image;
		if (SystemUtils.IS_OS_MAC_OSX && isMacMenuBarDarkMode()) {
			image = Toolkit.getDefaultToolkit().getImage(TrayIconUtil.class.getResource("/tray_icon_white.png"));
		} else {
			image = Toolkit.getDefaultToolkit().getImage(TrayIconUtil.class.getResource("/tray_icon.png"));
		}

		return new TrayIcon(image, rb.getString("app.name"), popup);
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
		final Runnable notificationCmd;
		if (SystemUtils.IS_OS_MAC_OSX) {
			final String title = rb.getString("tray.infoMsg.title");
			final String msg = rb.getString("tray.infoMsg.msg.osx");
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
			final String title = rb.getString("tray.infoMsg.title");
			final String msg = rb.getString("tray.infoMsg.msg");
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
			mainApplicationWindow.show();
			mainApplicationWindow.requestFocus();
		});
	}

	private void quitFromTray(ActionEvent event) {
		exitCommand.run();
	}

}
