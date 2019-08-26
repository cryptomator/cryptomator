package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;

import javax.inject.Inject;
import java.awt.Image;
import java.awt.Toolkit;

@TrayMenuScoped
class TrayImageFactory {

	//	private final Optional<MacFunctions> macFunctions;
	private final Settings settings;


	@Inject
//	TrayImageFactory(Optional<MacFunctions> macFunctions) {
//			this.macFunctions = macFunctions;
	TrayImageFactory(Settings settings) {
		this.settings = settings;
	}

	public Image loadImage() {
		String resourceName = SystemUtils.IS_OS_MAC_OSX ? getMacResourceName() : getWinOrLinuxResourceName();
		return Toolkit.getDefaultToolkit().getImage(getClass().getResource(resourceName));
	}

	private String getMacResourceName() {
//		MacApplicationUiInterfaceStyle interfaceStyle = macFunctions.map(MacFunctions::uiAppearance) //
//				.map(MacApplicationUiAppearance::getCurrentInterfaceStyle) //
//				.orElse(MacApplicationUiInterfaceStyle.LIGHT);
//			switch (interfaceStyle) {
		switch (settings.theme().get()) {
			case DARK:
				return "/tray_icon_mac_white.png";
			default:
				return "/tray_icon_mac_black.png";
		}
	}

	private String getWinOrLinuxResourceName() {
		return "/tray_icon.png";
	}

}
