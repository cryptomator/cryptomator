package org.cryptomator.ui.settings;

import java.util.Enumeration;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Localization extends ResourceBundle {

	@Inject
	public Localization() {
		this.parent = ResourceBundle.getBundle("localization");
	}

	@Override
	protected Object handleGetObject(String key) {
		return parent.getObject(key);
	}

	@Override
	public Enumeration<String> getKeys() {
		return parent.getKeys();
	}

}
