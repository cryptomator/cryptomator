/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.ui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import org.apache.commons.lang3.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sebastianstenzel.oce.ui.settings.Settings;

public class AdvancedController implements Initializable {

	private static final Logger LOG = LoggerFactory.getLogger(AdvancedController.class);

	@FXML
	private GridPane rootGridPane;

	@FXML
	private TextField portTextField;

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		portTextField.setText(String.valueOf(Settings.load().getPort()));
		portTextField.addEventFilter(KeyEvent.KEY_TYPED, new NumericKeyTypeEventFilter());
		portTextField.focusedProperty().addListener(new PortTextFieldFocusListener());
	}

	/**
	 * Consumes key events, if typed key is not 0-9.
	 */
	private static final class NumericKeyTypeEventFilter implements EventHandler<KeyEvent> {
		@Override
		public void handle(KeyEvent t) {
			if (t.getCharacter() == null || t.getCharacter().length() == 0) {
				return;
			}
			char c = t.getCharacter().charAt(0);
			if (!CharUtils.isAsciiNumeric(c)) {
				t.consume();
			}
		}
	}

	/**
	 * Saves port settings, when textfield loses focus.
	 */
	private class PortTextFieldFocusListener implements ChangeListener<Boolean> {
		@Override
		public void changed(ObservableValue<? extends Boolean> property, Boolean wasFocused, Boolean isFocused) {
			final Settings settings = Settings.load();
			try {
				int port = Integer.valueOf(portTextField.getText());
				settings.setPort(port);
			} catch (NumberFormatException ex) {
				LOG.warn("Invalid port " + portTextField.getText());
				portTextField.setText(String.valueOf(settings.getPort()));
			}
		}
	}

}
