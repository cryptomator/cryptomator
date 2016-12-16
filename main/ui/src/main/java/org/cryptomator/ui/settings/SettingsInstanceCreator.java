package org.cryptomator.ui.settings;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import com.google.gson.InstanceCreator;

class SettingsInstanceCreator implements InstanceCreator<Settings> {

	private final Consumer<Settings> saveCmd;

	public SettingsInstanceCreator(Consumer<Settings> saveCmd) {
		this.saveCmd = saveCmd;
	}

	@Override
	public Settings createInstance(Type type) {
		return new Settings(saveCmd);
	}

}
