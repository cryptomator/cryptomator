package org.cryptomator.common.streams;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class AutoClosingStreamFactory implements DelegatingStreamFactory {

	public static final DelegatingStreamFactory AUTO_CLOSING_STREAM_FACTORY = new AutoClosingStreamFactory();

	private AutoClosingStreamFactory() {
	}

	@Override
	public <S> Stream<S> from(Stream<S> other) {
		if (AutoClosingStream.class.isInstance(other)) {
			return other;
		} else {
			return AutoClosingStream.from(other);
		}
	}

	@Override
	public IntStream from(IntStream other) {
		if (AutoClosingIntStream.class.isInstance(other)) {
			return other;
		} else {
			return AutoClosingIntStream.from(other);
		}
	}

	@Override
	public LongStream from(LongStream other) {
		if (AutoClosingLongStream.class.isInstance(other)) {
			return other;
		} else {
			return AutoClosingLongStream.from(other);
		}
	}

	@Override
	public DoubleStream from(DoubleStream other) {
		if (AutoClosingDoubleStream.class.isInstance(other)) {
			return other;
		} else {
			return AutoClosingDoubleStream.from(other);
		}
	}

}
