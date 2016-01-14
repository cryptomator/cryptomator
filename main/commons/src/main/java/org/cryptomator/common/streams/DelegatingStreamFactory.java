package org.cryptomator.common.streams;

import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface DelegatingStreamFactory {

	public static DelegatingStreamFactory of( //
			ObjectStreamWrapper objectStreamWrapper, //
			Function<IntStream, IntStream> intStreamWrapper, //
			Function<LongStream, LongStream> longStreamWrapper, //
			Function<DoubleStream, DoubleStream> doubleStreamWrapper) {
		return new DelegatingStreamFactory() {
			@Override
			public DoubleStream from(DoubleStream other) {
				return doubleStreamWrapper.apply(other);
			}

			@Override
			public LongStream from(LongStream other) {
				return longStreamWrapper.apply(other);
			}

			@Override
			public IntStream from(IntStream other) {
				return intStreamWrapper.apply(other);
			}

			@Override
			public <S> Stream<S> from(Stream<S> other) {
				return objectStreamWrapper.from(other);
			}
		};
	}

	<S> Stream<S> from(Stream<S> other);

	IntStream from(IntStream other);

	LongStream from(LongStream other);

	DoubleStream from(DoubleStream other);

	public interface ObjectStreamWrapper {
		<S> Stream<S> from(Stream<S> other);
	}

}