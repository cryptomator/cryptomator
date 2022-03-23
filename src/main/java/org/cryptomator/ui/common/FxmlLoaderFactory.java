package org.cryptomator.ui.common;

import javax.inject.Provider;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

public class FxmlLoaderFactory {

	private final Map<Class<? extends FxController>, Provider<FxController>> controllerFactories;
	private final Function<Parent, Scene> sceneFactory;
	private final ResourceBundle resourceBundle;

	public FxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> controllerFactories, Function<Parent, Scene> sceneFactory, ResourceBundle resourceBundle) {
		this.controllerFactories = controllerFactories;
		this.sceneFactory = sceneFactory;
		this.resourceBundle = resourceBundle;
	}

	public static <T extends FxController> FxmlLoaderFactory forController(T controller, Function<Parent, Scene> sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(Map.of(controller.getClass(), () -> controller), sceneFactory, resourceBundle);
	}

	/**
	 * @return A new FXMLLoader instance
	 */
	private FXMLLoader construct(String fxmlResourceName) {
		var url = getClass().getResource(fxmlResourceName);
		return new FXMLLoader(url, resourceBundle, null, this::constructController);
	}

	/**
	 * Loads the FXML given fxml resource in a new FXMLLoader instance.
	 *
	 * @param fxmlResourceName Name of the resource (as in {@link Class#getResource(String)}).
	 * @return The FXMLLoader used to load the file
	 * @throws IOException if an error occurs while loading the FXML file
	 */
	public FXMLLoader load(String fxmlResourceName) throws IOException {
		FXMLLoader loader = construct(fxmlResourceName);
		loader.load();
		return loader;
	}

	public Scene createScene(FxmlFile fxmlFile) {
		return createScene(fxmlFile.getRessourcePathString());
	}

	/**
	 * {@link #load(String) Loads} the FXML file and creates a new Scene containing the loaded ui.
	 *
	 * @param fxmlResourceName Name of the resource (as in {@link Class#getResource(String)}).
	 * @throws UncheckedIOException wrapping any IOException thrown by {@link #load(String)).
	 */
	private Scene createScene(String fxmlResourceName) {
		final FXMLLoader loader;
		try {
			loader = load(fxmlResourceName);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load " + fxmlResourceName, e);
		}
		Parent root = loader.getRoot();
		// TODO: discuss if we can remove language-specific stylesheets
		// List<String> additionalStyleSheets = Splitter.on(',').omitEmptyStrings().splitToList(resourceBundle.getString("additionalStyleSheets"));
		// additionalStyleSheets.forEach(styleSheet -> root.getStylesheets().add("/css/" + styleSheet));
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
