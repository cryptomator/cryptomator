package org.cryptomator.common;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObservableUtil {

	public static <T, U> ObservableValue<U> mapWithDefault(ObservableValue<T> observable, Function<? super T, ? extends U> mapper, U defaultValue) {
		return Bindings.createObjectBinding(() -> {
			if (observable.getValue() == null) {
				return defaultValue;
			} else {
				return mapper.apply(observable.getValue());
			}
		}, observable);
	}

	public static <T, U> ObservableValue<U> mapWithDefault(ObservableValue<T> observable, Function<? super T, ? extends U> mapper, Supplier<U> defaultValue) {
		return Bindings.createObjectBinding(() -> {
			if (observable.getValue() == null) {
				return defaultValue.get();
			} else {
				return mapper.apply(observable.getValue());
			}
		}, observable);
	}
}
