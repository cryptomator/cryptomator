package org.cryptomator.common;

public class UncheckedInterruptedException extends RuntimeException {

	public UncheckedInterruptedException(InterruptedException e) {
		super(e);
	}

	@Override
	public InterruptedException getCause() {
		return (InterruptedException) super.getCause();
	}

}
