package org.cryptomator.ui.traymenu;

import javafx.beans.Observable;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

		if (SystemUtils.IS_OS_WINDOWS) {
			// TODO: test on windows: is this a double click?
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

	private void themeChanged(@SuppressWarnings("unused") Observable observable) {
		trayIcon.setImage(imageFactory.loadImage());
	}

}
