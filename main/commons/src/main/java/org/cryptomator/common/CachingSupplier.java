package org.cryptomator.common;

import java.util.function.Supplier;

public class CachingSupplier<T> implements Supplier<T> {

	public static <T> Supplier<T> from(Supplier<T> delegate) {
		return new CachingSupplier<>(delegate);
	}

	private Supplier<T> delegate;

	private CachingSupplier(Supplier<T> delegate) {
		this.delegate = () -> {
			T result = delegate.get();
			CachingSupplier.this.delegate = () -> result;
			return result;
		};
	}

	@Override
	public T get() {
		return delegate.get();
	}

}
