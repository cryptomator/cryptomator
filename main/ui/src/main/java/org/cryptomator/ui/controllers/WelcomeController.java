/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class WelcomeController implements Initializable {

	@FXML
	private ImageView botImageView;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.botImageView.setImage(new Image(WelcomeController.class.getResource("/bot_welcome.png").toString()));
	}

}
