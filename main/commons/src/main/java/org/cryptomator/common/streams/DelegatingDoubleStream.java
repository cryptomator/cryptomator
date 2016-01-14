package org.cryptomator.common.streams;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

abstract class DelegatingDoubleStream implements DoubleStream {

	private final DoubleStream delegate;
	private final DelegatingStreamFactory wrapper;

	public DelegatingDoubleStream(DoubleStream delegate, DelegatingStreamFactory wrapper) {
		this.delegate = delegate;
		this.wrapper = wrapper;
	}

	public DoubleStream filter(DoublePredicate predicate) {
		return wrapper.from(delegate.filter(predicate));
	}

	public boolean isParallel() {
		return delegate.isParallel();
	}

	public DoubleStream map(DoubleUnaryOperator mapper) {
		return wrapper.from(delegate.map(mapper));
	}

	public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
		return wrapper.from(delegate.mapToObj(mapper));
	}

	public DoubleStream unordered() {
		return wrapper.from(delegate.unordered());
	}

	public DoubleStream onClose(Runnable closeHandler) {
		return wrapper.from(delegate.onClose(closeHandler));
	}

	public IntStream mapToInt(DoubleToIntFunction mapper) {
		return wrapper.from(delegate.mapToInt(mapper));
	}

	public LongStream mapToLong(DoubleToLongFunction mapper) {
		return wrapper.from(delegate.mapToLong(mapper));
	}

	public void close() {
		delegate.close();
	}

	public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
		return wrapper.from(delegate.flatMap(mapper));
	}

	public DoubleStream distinct() {
		return wrapper.from(delegate.distinct());
	}

	public DoubleStream sorted() {
		return wrapper.from(delegate.sorted());
	}

	public DoubleStream peek(DoubleConsumer action) {
		return wrapper.from(delegate.peek(action));
	}

	public DoubleStream limit(long maxSize) {
		return wrapper.from(delegate.limit(maxSize));
	}

	public DoubleStream skip(long n) {
		return wrapper.from(delegate.skip(n));
	}

	public void forEach(DoubleConsumer action) {
		delegate.forEach(action);
	}

	public void forEachOrdered(DoubleConsumer action) {
		delegate.forEachOrdered(action);
	}

	public double[] toArray() {
		return delegate.toArray();
	}

	public double reduce(double identity, DoubleBinaryOperator op) {
		return delegate.reduce(identity, op);
	}

	public OptionalDouble reduce(DoubleBinaryOperator op) {
		return delegate.reduce(op);
	}

	public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		return delegate.collect(supplier, accumulator, combiner);
	}

	public double sum() {
		return delegate.sum();
	}

	public OptionalDouble min() {
		return delegate.min();
	}

	public OptionalDouble max() {
		return delegate.max();
	}

	public long count() {
		return delegate.count();
	}

	public OptionalDouble average() {
		return delegate.average();
	}

	public DoubleSummaryStatistics summaryStatistics() {
		return delegate.summaryStatistics();
	}

	public boolean anyMatch(DoublePredicate predicate) {
		return delegate.anyMatch(predicate);
	}

	public boolean allMatch(DoublePredicate predicate) {
		return delegate.allMatch(predicate);
	}

	public boolean noneMatch(DoublePredicate predicate) {
		return delegate.noneMatch(predicate);
	}

	public OptionalDouble findFirst() {
		return delegate.findFirst();
	}

	public OptionalDouble findAny() {
		return delegate.findAny();
	}

	public Stream<Double> boxed() {
		return wrapper.from(delegate.boxed());
	}

	public DoubleStream sequential() {
		return wrapper.from(delegate.sequential());
	}

	public DoubleStream parallel() {
		return wrapper.from(delegate.parallel());
	}

	public OfDouble iterator() {
		return delegate.iterator();
	}

	public java.util.Spliterator.OfDouble spliterator() {
		return delegate.spliterator();
	}

}
