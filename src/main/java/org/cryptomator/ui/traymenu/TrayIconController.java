package org.cryptomator.ui.traymenu;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;

@TrayMenuScoped
public class TrayIconController {

	private static final Logger LOG = LoggerFactory.getLogger(TrayIconController.class);

	private final TrayMenuController trayMenuController;
	private final TrayIcon trayIcon;
	private volatile boolean initialized;

	@Inject
	TrayIconController(TrayImageFactory imageFactory, TrayMenuController trayMenuController) {
		this.trayMenuController = trayMenuController;
		this.trayIcon = new TrayIcon(imageFactory.loadImage(), "Cryptomator", trayMenuController.getMenu());
	}

	public synchronized void initializeTrayIcon() throws IllegalStateException {
		Preconditions.checkState(!initialized);

		trayIcon.setImageAutoSize(true);
		if (SystemUtils.IS_OS_WINDOWS) {
			trayIcon.addActionListener(trayMenuController::showMainWindow);
		}

		try {
			SystemTray.getSystemTray().add(trayIcon);
			LOG.debug("initialized tray icon");
		} catch (AWTException e) {
			LOG.error("Error adding tray icon", e);
		}

		trayMenuController.initTrayMenu();

		this.initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}
}
