package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacApplicationUiAppearance;
import org.cryptomator.jni.MacApplicationUiInterfaceStyle;
import org.cryptomator.jni.MacFunctions;

import javax.inject.Inject;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Optional;

@TrayMenuScoped
class TrayImageFactory {

	private final Optional<MacFunctions> macFunctions;

	@Inject
	TrayImageFactory(Optional<MacFunctions> macFunctions) {
		this.macFunctions = macFunctions;
	}

	public Image loadImage() {
		String resourceName = SystemUtils.IS_OS_MAC_OSX ? getMacResourceName() : getWinOrLinuxResourceName();
		return Toolkit.getDefaultToolkit().getImage(getClass().getResource(resourceName));
	}

	private String getMacResourceName() {
		MacApplicationUiInterfaceStyle interfaceStyle = macFunctions.map(MacFunctions::uiAppearance) //
				.map(MacApplicationUiAppearance::getCurrentInterfaceStyle) //
				.orElse(MacApplicationUiInterfaceStyle.LIGHT);
		return switch (interfaceStyle) {
			case DARK -> "/img/tray_icon_mac_white.png";
			case LIGHT -> "/img/tray_icon_mac_black.png";
		};
	}

	private String getWinOrLinuxResourceName() {
		return "/img/tray_icon.png";
	}

}
