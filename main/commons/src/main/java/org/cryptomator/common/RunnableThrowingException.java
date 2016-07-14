package org.cryptomator.common;

@FunctionalInterface
public interface RunnableThrowingException<T extends Throwable> {

	void run() throws T;

}
