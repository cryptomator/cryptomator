package org.cryptomator.common.streams;

import static org.cryptomator.common.streams.AutoClosingStreamFactory.AUTO_CLOSING_STREAM_FACTORY;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * <p>
 * A Stream which is automatically closed after execution of a terminal operation.
 * <p>
 * Streams returned by intermediate operations are also auto closing.
 * <p>
 * <b>Note:</b> When using {@link #iterator()} or {@link #spliterator()} auto closing does not occur.
 * 
 * @author Markus Kreusch
 */
public class AutoClosingStream<T> extends DelegatingStream<T> {

	public static <T> Stream<T> from(Stream<T> delegate) {
		return new AutoClosingStream<>(delegate);
	}

	private AutoClosingStream(Stream<T> delegate) {
		super(delegate, AUTO_CLOSING_STREAM_FACTORY);
	}

	public void forEach(Consumer<? super T> action) {
		try {
			super.forEach(action);
		} finally {
			close();
		}
	}

	public void forEachOrdered(Consumer<? super T> action) {
		try {
			super.forEachOrdered(action);
		} finally {
			close();
		}
	}

	public Object[] toArray() {
		try {
			return super.toArray();
		} finally {
			close();
		}
	}

	public <A> A[] toArray(IntFunction<A[]> generator) {
		try {
			return super.toArray(generator);
		} finally {
			close();
		}
	}

	public T reduce(T identity, BinaryOperator<T> accumulator) {
		try {
			return super.reduce(identity, accumulator);
		} finally {
			close();
		}
	}

	public Optional<T> reduce(BinaryOperator<T> accumulator) {
		try {
			return super.reduce(accumulator);
		} finally {
			close();
		}
	}

	public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
		try {
			return super.reduce(identity, accumulator, combiner);
		} finally {
			close();
		}
	}

	public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
		try {
			return super.collect(supplier, accumulator, combiner);
		} finally {
			close();
		}
	}

	public <R, A> R collect(Collector<? super T, A, R> collector) {
		try {
			return super.collect(collector);
		} finally {
			close();
		}
	}

	public Optional<T> min(Comparator<? super T> comparator) {
		try {
			return super.min(comparator);
		} finally {
			close();
		}
	}

	public Optional<T> max(Comparator<? super T> comparator) {
		try {
			return super.max(comparator);
		} finally {
			close();
		}
	}

	public long count() {
		try {
			return super.count();
		} finally {
			close();
		}
	}

	public boolean anyMatch(Predicate<? super T> predicate) {
		try {
			return super.anyMatch(predicate);
		} finally {
			close();
		}
	}

	public boolean allMatch(Predicate<? super T> predicate) {
		try {
			return super.allMatch(predicate);
		} finally {
			close();
		}
	}

	public boolean noneMatch(Predicate<? super T> predicate) {
		try {
			return super.noneMatch(predicate);
		} finally {
			close();
		}
	}

	public Optional<T> findFirst() {
		try {
			return super.findFirst();
		} finally {
			close();
		}
	}

	public Optional<T> findAny() {
		try {
			return super.findAny();
		} finally {
			close();
		}
	}

}
