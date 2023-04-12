package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.tray.ActionItem;
import org.cryptomator.integrations.tray.SeparatorItem;
import org.cryptomator.integrations.tray.SubMenuItem;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.integrations.tray.TrayMenuException;
import org.cryptomator.integrations.tray.TrayMenuItem;
import org.purejava.linux.MemoryAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SystemTray;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySession;
import java.util.List;

import static org.purejava.linux.app_indicator_h.*;

@CheckAvailability
public class AppindicatorTrayMenuController implements TrayMenuController {

	private static final Logger LOG = LoggerFactory.getLogger(AppindicatorTrayMenuController.class);

	private final MemorySession session = MemorySession.openShared();
	private MemoryAddress indicator;
	private MemoryAddress menu = gtk_menu_new();

	@CheckAvailability
	public static boolean isAvailable() {
		return SystemUtils.IS_OS_LINUX && SystemTray.isSupported();
	}

	@Override
	public void showTrayIcon(byte[] bytes, String icon, Runnable runnable, String s) throws TrayMenuException {
		indicator = app_indicator_new(MemoryAllocator.ALLOCATE_FOR("org.cryptomator.Cryptomator"),
				MemoryAllocator.ALLOCATE_FOR(icon),
				APP_INDICATOR_CATEGORY_APPLICATION_STATUS());
		gtk_widget_show_all(menu);
		app_indicator_set_menu(indicator, menu);
		app_indicator_set_status(indicator, APP_INDICATOR_STATUS_ACTIVE());
	}

	@Override
	public void updateTrayIcon(byte[] bytes, String icon) {
		app_indicator_set_icon(indicator, MemoryAllocator.ALLOCATE_FOR(icon));
	}

	@Override
	public void updateTrayMenu(List<TrayMenuItem> items) throws TrayMenuException {
		menu = gtk_menu_new();
		addChildren(menu, items);
		gtk_widget_show_all(menu);
		app_indicator_set_menu(indicator, menu);
	}

	@Override
	public void onBeforeOpenMenu(Runnable runnable) {

	}

	private void addChildren(MemoryAddress menu, List<TrayMenuItem> items) {
		for (var item : items) {
			// TODO: use Pattern Matching for switch, once available
			if (item instanceof ActionItem a) {
				var gtkMenuItem = gtk_menu_item_new();
				gtk_menu_item_set_label(gtkMenuItem, MemoryAllocator.ALLOCATE_FOR(a.title()));
				g_signal_connect_object(gtkMenuItem,
						MemoryAllocator.ALLOCATE_FOR("activate"),
						MemoryAllocator.ALLOCATE_CALLBACK_FOR(new ActionItemCallback(a), session),
						menu,
						0);
				gtk_menu_shell_append(menu, gtkMenuItem);
			} else if (item instanceof SeparatorItem) {
				var gtkSeparator = gtk_menu_item_new();
				gtk_menu_shell_append(menu, gtkSeparator);
			} else if (item instanceof SubMenuItem s) {
				var gtkMenuItem = gtk_menu_item_new();
				var gtkSubmenu = gtk_menu_new();
				gtk_menu_item_set_label(gtkMenuItem, MemoryAllocator.ALLOCATE_FOR(s.title()));
				addChildren(gtkSubmenu, s.items());
				gtk_menu_item_set_submenu(gtkMenuItem, gtkSubmenu);
				gtk_menu_shell_append(menu, gtkMenuItem);
			}
			gtk_widget_show_all(menu);
		}
	}
}
