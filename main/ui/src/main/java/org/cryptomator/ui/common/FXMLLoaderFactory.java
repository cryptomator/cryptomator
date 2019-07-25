package org.cryptomator.ui.common;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.ResourceBundle;

public class FXMLLoaderFactory {

	private final Map<Class<? extends FxController>, Provider<FxController>> factories;
	private final ResourceBundle resourceBundle;

	public FXMLLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		this.factories = factories;
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
	 * {@link #load(String) Loads} the FXML file and sets the given stage's scene to a new Scene containing the loaded ui.
	 * @param fxmlResourceName Name of the resource (as in {@link Class#getResource(String)}).
	 * @param stage The stage which should get a new scene
	 * @throws UncheckedIOException wrapping any IOException thrown by {@link #load(String)).
	 */
	public void setScene(String fxmlResourceName, Stage stage) throws UncheckedIOException {
		final FXMLLoader loader;
		try {
			loader = load(fxmlResourceName);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load " + fxmlResourceName, e);
		}		Parent root = loader.getRoot();
		Scene scene = new Scene(root);
		stage.setScene(scene);
	}

	private FxController constructController(Class<?> aClass) {
		if (!factories.containsKey(aClass)) {
			throw new IllegalArgumentException("ViewController not registered: " + aClass);
		} else {
			return factories.get(aClass).get();
		}
	}
}
