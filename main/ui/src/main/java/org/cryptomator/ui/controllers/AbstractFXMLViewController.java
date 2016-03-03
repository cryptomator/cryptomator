/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.LazyInitializer;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Controller presenting a single view.
 */
abstract class AbstractFXMLViewController implements Initializable {

	private final AtomicReference<Parent> fxmlRoot = new AtomicReference<>();

	/**
	 * Gets the URL to the FXML file describing the view presented by this controller.<br/>
	 * 
	 * A default implementation would look like this:<br/>
	 * <code>
	 * 	return getClass().getResource("/myView.fxml");
	 * </code>
	 * 
	 * @return FXML resource URL
	 */
	protected abstract URL getFxmlResourceUrl();

	/**
	 * @return Localization bundle for the FXML labels or <code>null</code>.
	 */
	protected abstract ResourceBundle getFxmlResourceBundle();

	@Override
	public final void initialize(URL location, ResourceBundle resources) {
		this.initialize();
	}

	protected void initialize() {
	}

	/**
	 * Creates a FXML loader used in {@link #loadFxml()}. This method can be overwritten for further loader customization.
	 * 
	 * @return Configured loader ready to load.
	 */
	protected FXMLLoader createFxmlLoader() {
		final URL fxmlUrl = getFxmlResourceUrl();
		final ResourceBundle rb = getFxmlResourceBundle();
		final FXMLLoader loader = new FXMLLoader(fxmlUrl, rb);
		loader.setController(this);
		return loader;
	}

	/**
	 * Loads the view presented by this controller from the FXML file return by {@link #getFxmlResourceUrl()}. This method can only be invoked once.
	 * 
	 * @return Parent view element.
	 */
	protected final Parent loadFxml() {
		return LazyInitializer.initializeLazily(fxmlRoot, () -> {
			final FXMLLoader loader = createFxmlLoader();
			try {
				return loader.load();
			} catch (IOException e) {
				throw new IllegalStateException("Could not load FXML file from location: " + loader.getLocation(), e);
			}
		});
	}

	/**
	 * Creates a new scene with the root node from the FXML file and applies it to the given stage.
	 */
	public void initStage(Stage stage) {
		final Parent root = loadFxml();
		stage.setScene(new Scene(root));
		stage.sizeToScene();
	}

	/**
	 * @return Creates a new stage and calls {@link #initStage(Stage)}.
	 */
	public Stage createStage() {
		final Stage stage = new Stage();
		initStage(stage);
		return stage;
	}

}
