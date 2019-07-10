package org.cryptomator.ui;

import javafx.fxml.FXMLLoader;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.ResourceBundle;

@FxApplicationScoped
public class FXMLLoaderFactory {

	private final Map<Class<? extends FxController>, Provider<FxController>> factories;
	private final ResourceBundle resourceBundle;

	@Inject
	FXMLLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories) {
		this.factories = factories;
		this.resourceBundle = ResourceBundle.getBundle("i18n.strings");
	}

	public FXMLLoader construct() {
		FXMLLoader loader = new FXMLLoader();
		loader.setControllerFactory(this::constructController);
		loader.setResources(resourceBundle);
		return loader;
	}

	public FXMLLoader load(String fxmlResourceName) throws IOException {
		FXMLLoader loader = construct();
		try (InputStream in = getClass().getResourceAsStream(fxmlResourceName)) {
			loader.load(in);
		}
		return loader;
	}

	private FxController constructController(Class<?> aClass) {
		if (!factories.containsKey(aClass)) {
			throw new IllegalArgumentException("ViewController not registered: " + aClass);
		} else {
			return factories.get(aClass).get();
		}
	}
}
