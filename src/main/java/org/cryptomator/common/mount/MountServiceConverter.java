package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountService;

import javafx.util.StringConverter;
import java.util.ResourceBundle;

public class MountServiceConverter extends StringConverter<MountService> {

	private final ResourceBundle resourceBundle;

	public MountServiceConverter(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

	@Override
	public String toString(MountService provider) {
		if (provider == null) {
			return resourceBundle.getString("preferences.volume.type.automatic");
		} else {
			return provider.displayName();
		}
	}

	@Override
	public MountService fromString(String string) {
		throw new UnsupportedOperationException();
	}
}