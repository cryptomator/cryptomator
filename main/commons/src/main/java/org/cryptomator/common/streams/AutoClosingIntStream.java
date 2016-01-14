package org.cryptomator.common.streams;

import static org.cryptomator.common.streams.AutoClosingStreamFactory.AUTO_CLOSING_STREAM_FACTORY;

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class AutoClosingIntStream extends DelegatingIntStream {

	public static IntStream from(IntStream delegate) {
		return new AutoClosingIntStream(delegate);
	}

	public AutoClosingIntStream(IntStream delegate) {
		super(delegate, AUTO_CLOSING_STREAM_FACTORY);
	}

	@Override
	public void forEach(IntConsumer action) {
		try {
			super.forEach(action);
		} finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(IntConsumer action) {
		try {
			super.forEachOrdered(action);
		} finally {
			close();
		}
	}

	@Override
	public int[] toArray() {
		try {
			return super.toArray();
		} finally {
			close();
		}
	}

	@Override
	public int reduce(int identity, IntBinaryOperator op) {
		try {
			return super.reduce(identity, op);
		} finally {
			close();
		}
	}

	@Override
	public OptionalInt reduce(IntBinaryOperator op) {
		try {
			return super.reduce(op);
		} finally {
			close();
		}
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return super.collect(supplier, accumulator, combiner);
		} finally {
			close();
		}
	}

	@Override
	public int sum() {
		try {
			return super.sum();
		} finally {
			close();
		}
	}

	@Override
	public OptionalInt min() {
		try {
			return super.min();
		} finally {
			close();
		}
	}

	@Override
	public OptionalInt max() {
		try {
			return super.max();
		} finally {
			close();
		}
	}

	@Override
	public long count() {
		try {
			return super.count();
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble average() {
		try {
			return super.average();
		} finally {
			close();
		}
	}

	@Override
	public IntSummaryStatistics summaryStatistics() {
		try {
			return super.summaryStatistics();
		} finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(IntPredicate predicate) {
		try {
			return super.anyMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean allMatch(IntPredicate predicate) {
		try {
			return super.allMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(IntPredicate predicate) {
		try {
			return super.noneMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public OptionalInt findFirst() {
		try {
			return super.findFirst();
		} finally {
			close();
		}
	}

	@Override
	public OptionalInt findAny() {
		try {
			return super.findAny();
		} finally {
			close();
		}
	}

}
