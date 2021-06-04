package org.cryptomator.ui.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FormattedString {

	private final StringProperty format = new SimpleStringProperty("");
	private final ObjectProperty<Object> arg1 = new SimpleObjectProperty<>();
	// add arg2, arg3, ... on demand
	private final StringBinding value;

	public FormattedString() {
		this.value = Bindings.createStringBinding(this::computeValue, format, arg1);
	}

	protected String computeValue() {
		return String.format(format.get(), arg1.get());
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

	public StringBinding valueProperty() {
		return value;
	}

	public String getValue() {
		return value.get();
	}
}
