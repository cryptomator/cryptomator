package org.cryptomator.ui.traymenu;

import javafx.beans.Observable;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;

@TrayMenuScoped
public class TrayIconController {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrayIconController.class);

	private final Settings settings;
	private final TrayImageFactory imageFactory;
	private final TrayMenuController trayMenuController;
	private final TrayIcon trayIcon;

	@Inject
	TrayIconController(Settings settings, TrayImageFactory imageFactory, TrayMenuController trayMenuController) {
		this.settings = settings;
		this.trayMenuController = trayMenuController;
		this.imageFactory = imageFactory;
		this.trayIcon = new TrayIcon(imageFactory.loadImage(), "Cryptomator", trayMenuController.getMenu());
	}
	
	public void initializeTrayIcon() {
		settings.theme().addListener(this::themeChanged);
		
		trayMenuController.initTrayMenu();

		try {
			SystemTray.getSystemTray().add(trayIcon);
			LOG.info("initialized tray icon");
		} catch (AWTException e) {
			LOG.error("Error adding tray icon", e);
		}
	}

	private void themeChanged(@SuppressWarnings("unused") Observable observable) {
		trayIcon.setImage(imageFactory.loadImage());
	}


}
