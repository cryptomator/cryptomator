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