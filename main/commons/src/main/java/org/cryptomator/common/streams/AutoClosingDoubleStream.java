package org.cryptomator.common.streams;

import static org.cryptomator.common.streams.AutoClosingStreamFactory.AUTO_CLOSING_STREAM_FACTORY;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

public class AutoClosingDoubleStream extends DelegatingDoubleStream {

	public static DoubleStream from(DoubleStream delegate) {
		return new AutoClosingDoubleStream(delegate);
	}

	public AutoClosingDoubleStream(DoubleStream delegate) {
		super(delegate, AUTO_CLOSING_STREAM_FACTORY);
	}

	@Override
	public void forEach(DoubleConsumer action) {
		try {
			super.forEach(action);
		} finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(DoubleConsumer action) {
		try {
			super.forEachOrdered(action);
		} finally {
			close();
		}
	}

	@Override
	public double[] toArray() {
		try {
			return super.toArray();
		} finally {
			close();
		}
	}

	@Override
	public double reduce(double identity, DoubleBinaryOperator op) {
		try {
			return super.reduce(identity, op);
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble reduce(DoubleBinaryOperator op) {
		try {
			return super.reduce(op);
		} finally {
			close();
		}
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return super.collect(supplier, accumulator, combiner);
		} finally {
			close();
		}
	}

	@Override
	public double sum() {
		try {
			return super.sum();
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble min() {
		try {
			return super.min();
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble max() {
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
	public DoubleSummaryStatistics summaryStatistics() {
		try {
			return super.summaryStatistics();
		} finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(DoublePredicate predicate) {
		try {
			return super.anyMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean allMatch(DoublePredicate predicate) {
		try {
			return super.allMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(DoublePredicate predicate) {
		try {
			return super.noneMatch(predicate);
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble findFirst() {
		try {
			return super.findFirst();
		} finally {
			close();
		}
	}

	@Override
	public OptionalDouble findAny() {
		try {
			return super.findAny();
		} finally {
			close();
		}
	}

}
