package org.cryptomator.common.streams;

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

abstract class DelegatingLongStream implements LongStream {

	private final LongStream delegate;
	private final DelegatingStreamFactory wrapper;

	public DelegatingLongStream(LongStream delegate, DelegatingStreamFactory wrapper) {
		this.delegate = delegate;
		this.wrapper = wrapper;
	}

	public LongStream filter(LongPredicate predicate) {
		return wrapper.from(delegate.filter(predicate));
	}

	public boolean isParallel() {
		return delegate.isParallel();
	}

	public LongStream map(LongUnaryOperator mapper) {
		return wrapper.from(delegate.map(mapper));
	}

	public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
		return wrapper.from(delegate.mapToObj(mapper));
	}

	public LongStream unordered() {
		return wrapper.from(delegate.unordered());
	}

	public LongStream onClose(Runnable closeHandler) {
		return wrapper.from(delegate.onClose(closeHandler));
	}

	public IntStream mapToInt(LongToIntFunction mapper) {
		return wrapper.from(delegate.mapToInt(mapper));
	}

	public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
		return wrapper.from(delegate.mapToDouble(mapper));
	}

	public void close() {
		delegate.close();
	}

	public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
		return wrapper.from(delegate.flatMap(mapper));
	}

	public LongStream distinct() {
		return wrapper.from(delegate.distinct());
	}

	public LongStream sorted() {
		return wrapper.from(delegate.sorted());
	}

	public LongStream peek(LongConsumer action) {
		return wrapper.from(delegate.peek(action));
	}

	public LongStream limit(long maxSize) {
		return wrapper.from(delegate.limit(maxSize));
	}

	public LongStream skip(long n) {
		return wrapper.from(delegate.skip(n));
	}

	public void forEach(LongConsumer action) {
		delegate.forEach(action);
	}

	public void forEachOrdered(LongConsumer action) {
		delegate.forEachOrdered(action);
	}

	public long[] toArray() {
		return delegate.toArray();
	}

	public long reduce(long identity, LongBinaryOperator op) {
		return delegate.reduce(identity, op);
	}

	public OptionalLong reduce(LongBinaryOperator op) {
		return delegate.reduce(op);
	}

	public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		return delegate.collect(supplier, accumulator, combiner);
	}

	public long sum() {
		return delegate.sum();
	}

	public OptionalLong min() {
		return delegate.min();
	}

	public OptionalLong max() {
		return delegate.max();
	}

	public long count() {
		return delegate.count();
	}

	public OptionalDouble average() {
		return delegate.average();
	}

	public LongSummaryStatistics summaryStatistics() {
		return delegate.summaryStatistics();
	}

	public boolean anyMatch(LongPredicate predicate) {
		return delegate.anyMatch(predicate);
	}

	public boolean allMatch(LongPredicate predicate) {
		return delegate.allMatch(predicate);
	}

	public boolean noneMatch(LongPredicate predicate) {
		return delegate.noneMatch(predicate);
	}

	public OptionalLong findFirst() {
		return delegate.findFirst();
	}

	public OptionalLong findAny() {
		return delegate.findAny();
	}

	public DoubleStream asDoubleStream() {
		return wrapper.from(delegate.asDoubleStream());
	}

	public Stream<Long> boxed() {
		return wrapper.from(delegate.boxed());
	}

	public LongStream sequential() {
		return wrapper.from(delegate.sequential());
	}

	public LongStream parallel() {
		return wrapper.from(delegate.parallel());
	}

	public OfLong iterator() {
		return delegate.iterator();
	}

	public java.util.Spliterator.OfLong spliterator() {
		return delegate.spliterator();
	}

}
