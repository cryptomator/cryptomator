package org.cryptomator.ui.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;

public class FormattedLabel extends Label {

	private final StringProperty format = new SimpleStringProperty("");
	private final ObjectProperty<Object> arg1 = new SimpleObjectProperty<>();
	private final ObjectProperty<Object> arg2 = new SimpleObjectProperty<>();
	private final ObjectProperty<Object> arg3 = new SimpleObjectProperty<>();
	// add arg4, arg5, ... on demand

	public FormattedLabel() {
		textProperty().bind(createStringBinding());
	}

	protected StringBinding createStringBinding() {
		return Bindings.createStringBinding(this::updateText, format, arg1, arg2, arg3);
	}

	private String updateText() {
		return String.format(format.get(), arg1.get(), arg2.get(), arg3.get());
	}

	/* Observables */

	public StringProperty formatProperty() {
		return format;
	}

	public String getFormat() {
		return format.get();
	}

	public void setFormat(String format) {
		this.format.set(format);
	}

	public ObjectProperty<Object> arg1Property() {
		return arg1;
	}

	public Object getArg1() {
		return arg1.get();
	}

	public void setArg1(Object arg1) {
		this.arg1.set(arg1);
	}

	public ObjectProperty<Object> arg2Property() {
		return arg2;
	}

	public Object getArg2() {
		return arg2.get();
	}

	public void setArg2(Object arg2) {
		this.arg2.set(arg2);
	}

	public ObjectProperty<Object> arg3Property() {
		return arg3;
	}

	public Object getArg3() {
		return arg3.get();
	}

	public void setArg3(Object arg3) {
		this.arg3.set(arg3);
	}
}
