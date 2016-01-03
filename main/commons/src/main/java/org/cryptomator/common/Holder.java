package org.cryptomator.common;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Holder<V> implements Supplier<V>, Consumer<V> {

	private final V initial;

	private V value;

	public <W extends V> Holder(W initial) {
		this.initial = initial;
		reset();
	}

	public V get() {
		return value;
	}

	public void set(V value) {
		this.value = value;
	}

	public void reset() {
		set(initial);
	}

	@Override
	public void accept(V value) {
		set(value);
	}

}
