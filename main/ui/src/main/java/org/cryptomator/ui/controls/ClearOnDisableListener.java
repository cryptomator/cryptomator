/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.controls;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextInputControl;

public class ClearOnDisableListener implements ChangeListener<Boolean> {

	final TextInputControl control;

	public ClearOnDisableListener(TextInputControl control) {
		this.control = control;
	}

	@Override
	public void changed(ObservableValue<? extends Boolean> property, Boolean wasDisabled, Boolean isDisabled) {
		if (isDisabled) {
			control.clear();
		}
	}

}