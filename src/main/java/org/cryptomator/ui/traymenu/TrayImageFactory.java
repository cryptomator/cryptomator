package org.cryptomator.ui.traymenu;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;

import javax.inject.Inject;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Optional;

@TrayMenuScoped
class TrayImageFactory {

	private final Optional<UiAppearanceProvider> appearanceProvider;

	@Inject
	TrayImageFactory(Optional<UiAppearanceProvider> appearanceProvider) {
		this.appearanceProvider = appearanceProvider;
	}

	public Image loadImage() {
		String resourceName = SystemUtils.IS_OS_MAC_OSX ? getMacResourceName() : getWinOrLinuxResourceName();
		return Toolkit.getDefaultToolkit().getImage(getClass().getResource(resourceName));
	}

	private String getMacResourceName() {
		return "/img/tray_icon_mac.png";
	}

	private String getWinOrLinuxResourceName() {
		return "/img/tray_icon.png";
	}

}
