package org.cryptomator.common;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class LazyInitializer {

	private LazyInitializer() {
	}

	/**
	 * Threadsafe lazy initialization pattern as proposed on http://stackoverflow.com/a/30247202/4014509
	 * 
	 * @param <T> Type of the value
	 * @param reference A reference to a maybe not yet initialized value.
	 * @param factory A factory providing a value for the reference, if it doesn't exist yet. The factory may be invoked multiple times, but only one result will survive.
	 * @return The initialized value
	 */
	public static <T> T initializeLazily(AtomicReference<T> reference, Supplier<T> factory) {
		final T existingInstance = reference.get();
		if (existingInstance != null) {
			return existingInstance;
		} else {
			final T newInstance = factory.get();
			if (reference.compareAndSet(null, newInstance)) {
				return newInstance;
			} else {
				return reference.get();
			}
		}
	}

}
