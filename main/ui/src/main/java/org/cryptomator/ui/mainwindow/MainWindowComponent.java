/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.mainwindow;

import dagger.Subcomponent;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MainWindowScoped
@Subcomponent(modules = {MainWindowModule.class})
public interface MainWindowComponent {
	
	@MainWindow
	Stage window();

	@MainWindow
	FXMLLoaderFactory fxmlLoaders();
	
	default void showMainWindow() {
		Stage stage = window();
		fxmlLoaders().setScene("/fxml/main_window.fxml", stage);
		stage.show();
	}

	@Subcomponent.Builder
	interface Builder {
		MainWindowComponent build();
	}

}
