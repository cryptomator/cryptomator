package org.cryptomator.ui.l10n;

public class LocalizationMock extends Localization {

	@Override
	public String handleGetObject(String key) {
		return key;
	}

}
