/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainController {
	
	@FXML
	private VBox rootVBox;
	
	@FXML
	private Pane initializePanel;
	
	@FXML
	private Pane accessPanel;
	
	@FXML
	private Pane advancedPanel;
	
	@FXML
	protected void showInitializePane(ActionEvent event) {
		showPanel(initializePanel);
	}
	
	@FXML
	protected void showAccessPane(ActionEvent event) {
		showPanel(accessPanel);
	}
	
	@FXML
	protected void showAdvancedPane(ActionEvent event) {
		showPanel(advancedPanel);
	}
	
	private void showPanel(Pane panel) {
		rootVBox.getChildren().remove(1);
		rootVBox.getChildren().add(panel);
		rootVBox.getScene().getWindow().sizeToScene();
	}

}
