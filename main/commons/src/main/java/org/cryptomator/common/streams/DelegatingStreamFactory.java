package org.cryptomator.common.streams;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface DelegatingStreamFactory {

	<S> Stream<S> from(Stream<S> other);

	IntStream from(IntStream other);

	LongStream from(LongStream other);

	DoubleStream from(DoubleStream other);

	public interface ObjectStreamWrapper {
		<S> Stream<S> from(Stream<S> other);
	}

}