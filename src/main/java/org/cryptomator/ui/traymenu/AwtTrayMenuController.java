package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.tray.ActionItem;
import org.cryptomator.integrations.tray.SeparatorItem;
import org.cryptomator.integrations.tray.SubMenuItem;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.integrations.tray.TrayMenuException;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Base64;
import java.util.List;

/**
 * Responsible to manage the tray icon on macOS and Windows using AWT.
 * For Linux, we use {@link AppindicatorTrayMenuController}
 */
@CheckAvailability
@Priority(Priority.FALLBACK)
public class AwtTrayMenuController implements TrayMenuController {

	private static final Logger LOG = LoggerFactory.getLogger(AwtTrayMenuController.class);

	private static final String DATA_URI_SCHEME = "data:image/png;base64,";
	private final PopupMenu menu = new PopupMenu();
	private TrayIcon trayIcon;

	@CheckAvailability
	public static boolean isAvailable() {
		return !SystemUtils.IS_OS_LINUX && SystemTray.isSupported();
	}

	@Override
	public void showTrayIcon(URI uri, Runnable defaultAction, String tooltip) throws TrayMenuException {
		var image = Toolkit.getDefaultToolkit().createImage(getImageBytes(uri));
		trayIcon = new TrayIcon(image, tooltip, menu);

		trayIcon.setImageAutoSize(true);
		if (SystemUtils.IS_OS_WINDOWS) {
			trayIcon.addActionListener(evt -> defaultAction.run());
		}

		try {
			SystemTray.getSystemTray().add(trayIcon);
			LOG.debug("initialized tray icon");
		} catch (AWTException e) {
			throw new TrayMenuException("Failed to add icon to system tray.", e);
		}
	}

	@Override
	public void updateTrayIcon(URI uri) {
		var image = Toolkit.getDefaultToolkit().createImage(getImageBytes(uri));
		trayIcon.setImage(image);
	}

	@Override
	public void updateTrayMenu(List<TrayMenuItem> items) {
		menu.removeAll();
		addChildren(menu, items);
	}

	@Override
	public void onBeforeOpenMenu(Runnable listener) {
		Preconditions.checkNotNull(this.trayIcon);
		this.trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				listener.run();
			}
		});
	}

	private void addChildren(Menu menu, List<TrayMenuItem> items) {
		for (var item : items) {
			switch (item) {
				case ActionItem a -> {
					var menuItem = new MenuItem(a.title());
					menuItem.addActionListener(evt -> a.action().run());
					menuItem.setEnabled(a.enabled());
					menu.add(menuItem);
				}
				case SeparatorItem separatorItem -> menu.addSeparator();
				case SubMenuItem s -> {
					var submenu = new Menu(s.title());
					addChildren(submenu, s.items());
					menu.add(submenu);
				}
			}
		}
	}

	private byte[] getImageBytes(URI uri) {
		var data = uri.toString().split(DATA_URI_SCHEME)[1];
		return Base64.getDecoder().decode(data);
	}
}
