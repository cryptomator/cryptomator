/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.mainwindow;

import dagger.Subcomponent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.model.Vault;

import java.io.IOException;
import java.io.UncheckedIOException;

@MainWindowScoped
@Subcomponent(modules = {MainWindowModule.class})
public interface MainWindowComponent {
	
	Stage mainWindow();

	FXMLLoaderFactory fxmlLoaders();
	
	default void showMainWindow() {
		try {
			Parent root = fxmlLoaders().load("/fxml/main_window.fxml").getRoot();
			Stage stage = mainWindow();
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to load main_window.fxml", e);
		}
	}

	@Subcomponent.Builder
	interface Builder {
		MainWindowComponent build();
	}

}
