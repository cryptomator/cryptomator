package org.cryptomator.common;

@FunctionalInterface
public interface RunnableThrowingException<T extends Exception> {

	void run() throws T;

}
