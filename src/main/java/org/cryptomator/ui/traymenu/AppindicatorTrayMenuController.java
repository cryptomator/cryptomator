package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.common.CheckAvailability;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.integrations.tray.TrayMenuException;
import org.cryptomator.integrations.tray.TrayMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.SystemTray;
import java.lang.foreign.MemoryAddress;
import java.util.List;

import static org.purejava.linux.app_indicator_h.gtk_menu_new;

@CheckAvailability
public class AppindicatorTrayMenuController implements TrayMenuController {

	private static final Logger LOG = LoggerFactory.getLogger(AppindicatorTrayMenuController.class);

	private final MemoryAddress menu = gtk_menu_new();

	@CheckAvailability
	public static boolean isAvailable() {
		return SystemUtils.IS_OS_LINUX && SystemTray.isSupported();
	}

	@Override
	public void showTrayIcon(byte[] bytes, Runnable runnable, String s) throws TrayMenuException {

	}

	@Override
	public void updateTrayIcon(byte[] bytes) {

	}

	@Override
	public void updateTrayMenu(List<TrayMenuItem> list) throws TrayMenuException {

	}

	@Override
	public void onBeforeOpenMenu(Runnable runnable) {

	}
}
