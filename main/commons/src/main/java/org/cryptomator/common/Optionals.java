/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import java.util.Optional;
import java.util.function.Function;

public final class Optionals {

	private Optionals() {
	}

	/**
	 * Returns a function that is equivalent to the input function but immediately gets the value of the returned optional when invoked.
	 * 
	 * @param <T> the type of the input to the function
	 * @param <R> the type of the result of the function
	 * @param function An {@code Optional}-bearing input function {@code Function<Foo, Optional<Bar>>}
	 * @return A {@code Function<Foo, Bar>}, that may throw a NoSuchElementException, if the original function returns an empty optional.
	 */
	public static <T, R> Function<T, R> unwrap(Function<T, Optional<R>> function) {
		return t -> function.apply(t).get();
	}

}
