package org.cryptomator.common;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import java.util.function.Function;

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
}
