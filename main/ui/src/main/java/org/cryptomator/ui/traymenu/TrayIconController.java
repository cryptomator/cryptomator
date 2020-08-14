package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.util.Optional;

@TrayMenuScoped
public class TrayIconController {

	private static final Logger LOG = LoggerFactory.getLogger(TrayIconController.class);

	private final TrayImageFactory imageFactory;
	private final TrayMenuController trayMenuController;
	private final TrayIcon trayIcon;
	private final Optional<MacFunctions> macFunctions;

	@Inject
	TrayIconController(TrayImageFactory imageFactory, TrayMenuController trayMenuController, Optional<MacFunctions> macFunctions) {
		this.trayMenuController = trayMenuController;
		this.imageFactory = imageFactory;
		this.trayIcon = new TrayIcon(imageFactory.loadImage(), "Cryptomator", trayMenuController.getMenu());
		this.macFunctions = macFunctions;
	}

	public void initializeTrayIcon() {
		macFunctions.map(MacFunctions::uiAppearance).ifPresent(uiAppearance -> uiAppearance.addListener(this::macInterfaceThemeChanged));

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
	}

	private void macInterfaceThemeChanged() {
		trayIcon.setImage(imageFactory.loadImage());
	}

}
