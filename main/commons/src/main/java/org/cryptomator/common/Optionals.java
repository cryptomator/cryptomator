package org.cryptomator.common;

import java.util.Optional;

public final class Optionals {

	private Optionals() {
	}

	public static <T, E extends Exception> void ifPresent(Optional<T> optional, ConsumerThrowingException<T, E> consumer) throws E {
		final T t = optional.orElse(null);
		if (t != null) {
			consumer.accept(t);
		}
	}

}
