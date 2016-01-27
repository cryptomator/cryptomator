package org.cryptomator.common;

@FunctionalInterface
public interface ConsumerThrowingException<T, E extends Exception> {

	void accept(T t) throws E;

}
