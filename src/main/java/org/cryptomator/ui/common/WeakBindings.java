package org.cryptomator.ui.common;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;


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

	/**
	 * Create a new LongBinding that listens to changes from the given observable without being strongly referenced by it.
	 *
	 * @param observable The observable
	 * @return a LongBinding weakly referenced from the given observable
	 */
	public static LongBinding bindLong(ObservableValue<Number> observable) {
		return new LongBinding() {
			{
				bind(observable);
			}

			@Override
			protected long computeValue() {
				return observable.getValue().longValue();
			}
		};
	}

	/**
	 * Create a new DoubleBinding that listens to changes from the given observable without being strongly referenced by it.
	 *
	 * @param observable The observable
	 * @return a DoubleBinding weakly referenced from the given observable
	 */
	public static DoubleBinding bindDouble(ObservableValue<Number> observable) {
		return new DoubleBinding() {
			{
				bind(observable);
			}

			@Override
			protected double computeValue() {
				return observable.getValue().doubleValue();
			}
		};
	}

	/**
	 * Create a new IntegerBinding that listens to changes from the given observable without being strongly referenced by it.
	 *
	 * @param observable The observable
	 * @return a IntegerBinding weakly referenced from the given observable
	 */
	public static IntegerBinding bindInterger(ObservableValue<Number> observable) {
		return new IntegerBinding() {
			{
				bind(observable);
			}

			@Override
			protected int computeValue() {
				return observable.getValue().intValue();
			}
		};
	}

}
