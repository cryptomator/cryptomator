package org.cryptomator.common;

@FunctionalInterface
public interface ConsumerThrowingException<T, E extends Throwable> {

	void accept(T t) throws E;

}
