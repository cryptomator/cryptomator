package org.cryptomator.common.streams;

import static org.cryptomator.common.streams.AutoClosingStreamFactory.AUTO_CLOSING_STREAM_FACTORY;

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;

public class AutoClosingLongStream extends DelegatingLongStream {

	public static LongStream from(LongStream delegate) {
		return new AutoClosingLongStream(delegate);
	}

	public AutoClosingLongStream(LongStream delegate) {
		super(delegate, AUTO_CLOSING_STREAM_FACTORY);
	}

	@Override
	public void forEach(LongConsumer action) {
		try {
			super.forEach(action);
		} finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(LongConsumer action) {
		try {
			super.forEachOrdered(action);
		} finally {
			close();
		}
	}

	@Override
	public long[] toArray() {
		try {
			return super.toArray();
		} finally {
			close();
		}
	}

	@Override
	public long reduce(long identity, LongBinaryOperator op) {
		try {
			return super.reduce(identity, op);
		} finally {
			close();
		}
	}

	@Override
	public OptionalLong reduce(LongBinaryOperator op) {
		try {
			return super.reduce(op);
		} finally {
			close();
		}
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return super.collect(supplier, accumulator, combiner);
		} finally {
			close();
		}
	}

	@Override
	public long sum() {
		try {
			return super.sum();
		} finally {
			close();
		}
	}

	@Override
	public OptionalLong min() {
		try {
			return super.min();
		} finally {
			close();
		}
	}

	@Override
	public OptionalLong max() {
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
	public LongSummaryStatistics summaryStatistics() {
		try {
			return super.summaryStatistics();
		} finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(LongPredicate predicate) {
		try {
			return super.anyMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean allMatch(LongPredicate predicate) {
		try {
			return super.allMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(LongPredicate predicate) {
		try {
			return super.noneMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public OptionalLong findFirst() {
		try {
			return super.findFirst();
		} finally {
			close();
		}
	}

	@Override
	public OptionalLong findAny() {
		try {
			return super.findAny();
		} finally {
			close();
		}
	}

}
