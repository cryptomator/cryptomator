/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Window;

public class ActiveWindowStyleSupport implements ChangeListener<Boolean> {

	public static final String ACTIVE_WINDOW_STYLE_CLASS = "active-window";
	public static final String INACTIVE_WINDOW_STYLE_CLASS = "inactive-window";

	private final Window window;

	private ActiveWindowStyleSupport(Window window) {
		this.window = window;
		this.addActiveWindowClassIfFocused(window.isFocused());
	}

	/**
	 * Creates and registers a listener on the given window, that will add the class {@value #ACTIVE_WINDOW_STYLE_CLASS} to the scenes root element, if the window is active. Otherwise
	 * {@value #INACTIVE_WINDOW_STYLE_CLASS} will be added. Allows CSS rules to be defined depending on the window's focus.<br/>
	 * <br/>
	 * Example:<br/>
	 * <code>
	 * .root.inactive-window .button {-fx-background-color: grey;}<br/>
	 * .root.active-window .button {-fx-background-color: blue;}
	 * </code>
	 * 
	 * @param window The window to observe
	 * @return The observer
	 */
	public static ChangeListener<Boolean> startObservingFocus(final Window window) {
		final ChangeListener<Boolean> observer = new ActiveWindowStyleSupport(window);
		window.focusedProperty().addListener(observer);
		return observer;
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		this.addActiveWindowClassIfFocused(newValue);
	}

	private void addActiveWindowClassIfFocused(Boolean focused) {
		if (Boolean.TRUE.equals(focused)) {
			window.getScene().getRoot().getStyleClass().add(ACTIVE_WINDOW_STYLE_CLASS);
			window.getScene().getRoot().getStyleClass().remove(INACTIVE_WINDOW_STYLE_CLASS);
		} else {
			window.getScene().getRoot().getStyleClass().remove(ACTIVE_WINDOW_STYLE_CLASS);
			window.getScene().getRoot().getStyleClass().add(INACTIVE_WINDOW_STYLE_CLASS);
		}
	}

}
