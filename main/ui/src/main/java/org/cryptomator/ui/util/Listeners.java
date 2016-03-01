package org.cryptomator.ui.util;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public final class Listeners {

	private Listeners() {
	}

	/**
	 * Invokes the given consumer with the new value as soon as a change is reported by an {@link ObservableValue}.
	 * 
	 * @param consumer The function to call with the changed value.
	 * @return A listener that can i.e. be used in {@link ObservableValue#addListener(ChangeListener)}.
	 */
	public static <T> ChangeListener<T> withNewValue(Consumer<T> consumer) {
		return (ObservableValue<? extends T> observable, T oldValue, T newValue) -> {
			consumer.accept(newValue);
		};
	}

}
