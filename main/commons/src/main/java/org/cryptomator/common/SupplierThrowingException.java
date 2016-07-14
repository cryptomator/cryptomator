package org.cryptomator.common;

@FunctionalInterface
public interface SupplierThrowingException<T, E extends Throwable> {

	T get() throws E;

}
