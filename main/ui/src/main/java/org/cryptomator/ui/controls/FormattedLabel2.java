package org.cryptomator.ui.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class FormattedLabel2 extends FormattedLabel {

	private final ObjectProperty<Object> arg2 = new SimpleObjectProperty<>();

	public FormattedLabel2() {
		textProperty().unbind();
		textProperty().bind(createStringBinding2());
	}

	private StringBinding createStringBinding2() {
		return Bindings.createStringBinding(this::updateText, formatProperty(), arg1Property(), arg2);
	}

	private String updateText() {
		return String.format(getFormat(), getArg1(), arg2.get());
	}

	/* Getter & Setter */

	public ObjectProperty<Object> arg2Property() {
		return arg2;
	}

	public Object getArg2() {
		return arg2.get();
	}

	public void setArg2(Object arg2) {
		this.arg2.set(arg2);
	}

}
