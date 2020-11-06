package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
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
	private final Optional<UiAppearanceProvider> appearanceProvider;
	private final TrayMenuController trayMenuController;
	private final TrayIcon trayIcon;

	@Inject
	TrayIconController(TrayImageFactory imageFactory, TrayMenuController trayMenuController, Optional<UiAppearanceProvider> appearanceProvider) {
		this.trayMenuController = trayMenuController;
		this.imageFactory = imageFactory;
		this.appearanceProvider = appearanceProvider;
		this.trayIcon = new TrayIcon(imageFactory.loadImage(), "Cryptomator", trayMenuController.getMenu());
	}

	public void initializeTrayIcon() {
		appearanceProvider.ifPresent(appearanceProvider -> {
			try {
				appearanceProvider.addListener(this::systemInterfaceThemeChanged);
			} catch (UiAppearanceException e) {
				LOG.error("Failed to enable automatic tray icon theme switching.");
			}
		});

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

	private void systemInterfaceThemeChanged(Theme theme) {
		trayIcon.setImage(imageFactory.loadImage()); // TODO refactor "theme" is re-queried in loadImage()
	}

}
