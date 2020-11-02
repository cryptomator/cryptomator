package org.cryptomator.ui.common;

import javax.inject.Provider;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class FXMLLoaderFactory {

	private final Map<Class<? extends FxController>, Provider<FxController>> controllerFactories;
	private final Function<Parent, Scene> sceneFactory;
	private final ResourceBundle resourceBundle;

	public FXMLLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> controllerFactories, Function<Parent, Scene> sceneFactory, ResourceBundle resourceBundle) {
		this.controllerFactories = controllerFactories;
		this.sceneFactory = sceneFactory;
		this.resourceBundle = resourceBundle;
	}

	/**
	 * @return A new FXMLLoader instance
	 */
	public FXMLLoader construct() {
		FXMLLoader loader = new FXMLLoader();
		loader.setControllerFactory(this::constructController);
		loader.setResources(resourceBundle);
		return loader;
	}

	/**
	 * Loads the FXML given fxml resource in a new FXMLLoader instance.
	 *
	 * @param fxmlResourceName Name of the resource (as in {@link Class#getResource(String)}).
	 * @return The FXMLLoader used to load the file
	 * @throws IOException if an error occurs while loading the FXML file
	 */
	public FXMLLoader load(String fxmlResourceName) throws IOException {
		FXMLLoader loader = construct();
		try (InputStream in = getClass().getResourceAsStream(fxmlResourceName)) {
			loader.load(in);
		}
		return loader;
	}

	/**
	 * {@link #load(String) Loads} the FXML file and creates a new Scene containing the loaded ui.
	 *
	 * @param fxmlResourceName Name of the resource (as in {@link Class#getResource(String)}).
	 * @throws UncheckedIOException wrapping any IOException thrown by {@link #load(String)).
	 */
	public Scene createScene(String fxmlResourceName) {
		final FXMLLoader loader;
		try {
			loader = load(fxmlResourceName);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load " + fxmlResourceName, e);
		}
		Parent root = loader.getRoot();
		// TODO: discuss if we can remove language-specific stylesheets
		// List<String> addtionalStyleSheets = Splitter.on(',').omitEmptyStrings().splitToList(resourceBundle.getString("additionalStyleSheets"));
		// addtionalStyleSheets.forEach(styleSheet -> root.getStylesheets().add("/css/" + styleSheet));
		return sceneFactory.apply(root);
	}

	private FxController constructController(Class<?> aClass) {
		if (!controllerFactories.containsKey(aClass)) {
			throw new IllegalArgumentException("ViewController not registered: " + aClass);
		} else {
			return controllerFactories.get(aClass).get();
		}
	}
}
