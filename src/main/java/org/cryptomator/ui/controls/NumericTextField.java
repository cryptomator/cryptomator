package org.cryptomator.ui.controls;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

public class NumericTextField extends TextField {

	private final static Pattern DIGIT_PATTERN = Pattern.compile("\\d*");

	public NumericTextField() {
		this.setTextFormatter(new TextFormatter<>(this::filterNumericTextChange));
	}

	private TextFormatter.Change filterNumericTextChange(TextFormatter.Change change) {
		return DIGIT_PATTERN.matcher(change.getText()).matches() ? change : null;
	}

	public int getAsIntUnchecked() {
		return Integer.valueOf(textProperty().get());
	}

	public Optional<Integer> getAsInt() {
		try {
			return Optional.of(Integer.valueOf(textProperty().get()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public void setText(int n) {
		textProperty().set(Integer.toString(n));
	}

}
