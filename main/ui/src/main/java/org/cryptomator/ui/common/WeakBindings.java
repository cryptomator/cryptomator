package org.cryptomator.ui.common;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableObjectValue;


/**
 * Contains a variety of method to create {@link java.util.function.Function#identity() identity}-bindings
 * to facilitate the Weak References used internally in JavaFX's Bindings.
 */
public final class WeakBindings {

	/**
	 * Create a new StringBinding that listens to changes from the given observable without being strongly referenced by it.
	 *
	 * @param observable The observable
	 * @return a StringBinding weakly referenced from the given observable
	 */
	public static StringBinding bindString(ObservableObjectValue<String> observable) {
		return new StringBinding() {
			{
				bind(observable);
			}

			@Override
			protected String computeValue() {
				return observable.get();
			}
		};
	}

}
