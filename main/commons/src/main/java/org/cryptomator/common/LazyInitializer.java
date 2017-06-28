/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.base.Throwables;

public final class LazyInitializer {

	private LazyInitializer() {
	}

	/**
	 * Same as {@link #initializeLazily(AtomicReference, SupplierThrowingException, Class)} except that no checked exception may be thrown by the factory function.
	 * 
	 * @param <T> Type of the value
	 * @param reference A reference to a maybe not yet initialized value.
	 * @param factory A factory providing a value for the reference, if it doesn't exist yet. The factory may be invoked multiple times, but only one result will survive.
	 * @return The initialized value
	 */
	public static <T> T initializeLazily(AtomicReference<T> reference, Supplier<T> factory) {
		SupplierThrowingException<T, RuntimeException> factoryThrowingRuntimeExceptions = () -> factory.get();
		return initializeLazily(reference, factoryThrowingRuntimeExceptions, RuntimeException.class);
	}

	/**
	 * Threadsafe lazy initialization pattern as proposed on http://stackoverflow.com/a/30247202/4014509
	 * 
	 * @param <T> Type of the value
	 * @param <E> Type of the any expected exception that may occur during initialization
	 * @param reference A reference to a maybe not yet initialized value.
	 * @param factory A factory providing a value for the reference, if it doesn't exist yet. The factory may be invoked multiple times, but only one result will survive.
	 * @param exceptionType Expected exception type.
	 * @return The initialized value
	 * @throws E Exception thrown by the factory function.
	 */
	public static <T, E extends Exception> T initializeLazily(AtomicReference<T> reference, SupplierThrowingException<T, E> factory, Class<E> exceptionType) throws E {
		final T existing = reference.get();
		if (existing != null) {
			return existing;
		} else {
			try {
				return reference.updateAndGet(invokeFactoryIfNull(factory));
			} catch (InitializationException e) {
				Throwables.throwIfUnchecked(e.getCause());
				Throwables.throwIfInstanceOf(e.getCause(), exceptionType);
				throw e;
			}
		}
	}

	private static <T, E extends Exception> UnaryOperator<T> invokeFactoryIfNull(SupplierThrowingException<T, E> factory) throws InitializationException {
		return currentValue -> {
			if (currentValue == null) {
				try {
					return factory.get();
				} catch (Exception e) {
					throw new InitializationException(e);
				}
			} else {
				return currentValue;
			}
		};
	}

	private static class InitializationException extends RuntimeException {

		public InitializationException(Throwable cause) {
			super(cause);
		}

	}
}
