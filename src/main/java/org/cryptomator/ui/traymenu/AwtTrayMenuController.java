package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.tray.ActionItem;
import org.cryptomator.integrations.tray.SeparatorItem;
import org.cryptomator.integrations.tray.SubMenuItem;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.integrations.tray.TrayMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Priority(Priority.FALLBACK)
public class AwtTrayMenuController implements TrayMenuController {

	private static final Logger LOG = LoggerFactory.getLogger(AwtTrayMenuController.class);

	private TrayIcon trayIcon;
	private PopupMenu menu = new PopupMenu();

	@Override
	public void showTrayIcon(InputStream rawImageData, Runnable defaultAction, String tooltip) throws IOException {
		var image = Toolkit.getDefaultToolkit().createImage(rawImageData.readAllBytes());
		trayIcon = new TrayIcon(image, tooltip, menu);

		trayIcon.setImageAutoSize(true);
		if (SystemUtils.IS_OS_WINDOWS) {
			trayIcon.addActionListener(evt -> defaultAction.run());
		}

		try {
			SystemTray.getSystemTray().add(trayIcon);
			LOG.debug("initialized tray icon");
		} catch (AWTException e) {
			LOG.error("Error adding tray icon", e);
		}
	}

	@Override
	public void updateTrayMenu(List<TrayMenuItem> items) {
		menu.removeAll();
		addChildren(menu, items);
	}

	private void addChildren(Menu menu, List<TrayMenuItem> items) {
		for (var item : items) {
			// TODO: use Pattern Matching for switch, once available
			if (item instanceof ActionItem a) {
				var menuItem = new MenuItem(a.title());
				menuItem.addActionListener(evt -> a.action().run());
				// TODO menuItem.setEnabled(a.enabled());
				menu.add(menuItem);
			} else if (item instanceof SeparatorItem) {
				menu.addSeparator();
			} else if (item instanceof SubMenuItem s) {
				var submenu = new Menu(s.title());
				addChildren(submenu, s.items());
				menu.add(submenu);
			}
		}
	}

}
