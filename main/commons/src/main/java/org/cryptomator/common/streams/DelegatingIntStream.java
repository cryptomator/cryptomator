package org.cryptomator.common.streams;

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

abstract class DelegatingIntStream implements IntStream {

	private final IntStream delegate;
	private final DelegatingStreamFactory wrapper;

	public DelegatingIntStream(IntStream delegate, DelegatingStreamFactory wrapper) {
		this.delegate = delegate;
		this.wrapper = wrapper;
	}

	public IntStream filter(IntPredicate predicate) {
		return wrapper.from(delegate.filter(predicate));
	}

	public boolean isParallel() {
		return delegate.isParallel();
	}

	public IntStream map(IntUnaryOperator mapper) {
		return wrapper.from(delegate.map(mapper));
	}

	public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
		return wrapper.from(delegate.mapToObj(mapper));
	}

	public IntStream unordered() {
		return wrapper.from(delegate.unordered());
	}

	public LongStream mapToLong(IntToLongFunction mapper) {
		return wrapper.from(delegate.mapToLong(mapper));
	}

	public IntStream onClose(Runnable closeHandler) {
		return wrapper.from(delegate.onClose(closeHandler));
	}

	public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
		return wrapper.from(delegate.mapToDouble(mapper));
	}

	public void close() {
		delegate.close();
	}

	public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
		return wrapper.from(delegate.flatMap(mapper));
	}

	public IntStream distinct() {
		return wrapper.from(delegate.distinct());
	}

	public IntStream sorted() {
		return wrapper.from(delegate.sorted());
	}

	public IntStream peek(IntConsumer action) {
		return wrapper.from(delegate.peek(action));
	}

	public IntStream limit(long maxSize) {
		return wrapper.from(delegate.limit(maxSize));
	}

	public IntStream skip(long n) {
		return wrapper.from(delegate.skip(n));
	}

	public void forEach(IntConsumer action) {
		delegate.forEach(action);
	}

	public void forEachOrdered(IntConsumer action) {
		delegate.forEachOrdered(action);
	}

	public int[] toArray() {
		return delegate.toArray();
	}

	public int reduce(int identity, IntBinaryOperator op) {
		return delegate.reduce(identity, op);
	}

	public OptionalInt reduce(IntBinaryOperator op) {
		return delegate.reduce(op);
	}

	public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		return delegate.collect(supplier, accumulator, combiner);
	}

	public int sum() {
		return delegate.sum();
	}

	public OptionalInt min() {
		return delegate.min();
	}

	public OptionalInt max() {
		return delegate.max();
	}

	public long count() {
		return delegate.count();
	}

	public OptionalDouble average() {
		return delegate.average();
	}

	public IntSummaryStatistics summaryStatistics() {
		return delegate.summaryStatistics();
	}

	public boolean anyMatch(IntPredicate predicate) {
		return delegate.anyMatch(predicate);
	}

	public boolean allMatch(IntPredicate predicate) {
		return delegate.allMatch(predicate);
	}

	public boolean noneMatch(IntPredicate predicate) {
		return delegate.noneMatch(predicate);
	}

	public OptionalInt findFirst() {
		return delegate.findFirst();
	}

	public OptionalInt findAny() {
		return delegate.findAny();
	}

	public LongStream asLongStream() {
		return wrapper.from(delegate.asLongStream());
	}

	public DoubleStream asDoubleStream() {
		return wrapper.from(delegate.asDoubleStream());
	}

	public Stream<Integer> boxed() {
		return wrapper.from(delegate.boxed());
	}

	public IntStream sequential() {
		return wrapper.from(delegate.sequential());
	}

	public IntStream parallel() {
		return wrapper.from(delegate.parallel());
	}

	public OfInt iterator() {
		return delegate.iterator();
	}

	public java.util.Spliterator.OfInt spliterator() {
		return delegate.spliterator();
	}

}
