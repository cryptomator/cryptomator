package org.cryptomator.common.streams;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(Theories.class)
public class AutoClosingDoubleStreamTest {

	private static final DoublePredicate A_DOUBLE_PREDICATE = any -> true;
	private static final DoubleFunction A_DOUBLE_FUNCTION = i -> null;
	private static final BiConsumer A_BICONSUMER = (a, b) -> {
	};
	private static final Supplier A_SUPPLIER = () -> null;

	@DataPoints("intermediateOperations")
	public static final List<DoubleermediateOperation<?>> INTERMEDIATE_OPERATIONS = new ArrayList<>();

	@DataPoints("terminalOperations")
	public static final List<TerminalOperation<?>> TERMINAL_OPERATIONS = new ArrayList<>();
	private static final DoubleUnaryOperator A_DOUBLE_UNARY_OPERATOR = i -> 3;
	private static final DoubleToLongFunction A_DOUBLE_TO_LONG_FUNCTION = i -> 3L;
	private static final DoubleToIntFunction A_DOUBLE_TO_INT_FUNCTION = i -> 5;
	private static final DoubleConsumer A_DOUBLE_CONSUMER = i -> {
	};
	private static final ObjDoubleConsumer AN_OBJ_DOUBLE_CONSUMER = (a, b) -> {
	};
	private static final DoubleBinaryOperator A_DOUBLE_BINARY_OPERATOR = (a, b) -> a;

	static {
		// define intermediate operations
		test(DoubleStream.class, DoubleStream::distinct);
		test(DoubleStream.class, stream -> stream.filter(A_DOUBLE_PREDICATE));
		test(DoubleStream.class, stream -> stream.flatMap(A_DOUBLE_FUNCTION));
		test(DoubleStream.class, stream -> stream.limit(5));
		test(DoubleStream.class, stream -> stream.map(A_DOUBLE_UNARY_OPERATOR));
		test(LongStream.class, stream -> stream.mapToLong(A_DOUBLE_TO_LONG_FUNCTION));
		test(Stream.class, stream -> stream.mapToObj(A_DOUBLE_FUNCTION));
		test(IntStream.class, stream -> stream.mapToInt(A_DOUBLE_TO_INT_FUNCTION));
		test(DoubleStream.class, DoubleStream::parallel);
		test(DoubleStream.class, stream -> stream.peek(A_DOUBLE_CONSUMER));
		test(DoubleStream.class, DoubleStream::sequential);
		test(DoubleStream.class, stream -> stream.skip(5));
		test(DoubleStream.class, DoubleStream::sorted);
		test(DoubleStream.class, DoubleStream::unordered);
		test(Stream.class, DoubleStream::boxed);

		// define terminal operations
		test(stream -> stream.allMatch(A_DOUBLE_PREDICATE), true);
		test(stream -> stream.anyMatch(A_DOUBLE_PREDICATE), true);
		test(stream -> stream.collect(A_SUPPLIER, AN_OBJ_DOUBLE_CONSUMER, A_BICONSUMER), 7d);
		test(DoubleStream::count, 3L);
		test(DoubleStream::findAny, OptionalDouble.of(3));
		test(DoubleStream::findFirst, OptionalDouble.of(3));
		test(stream -> stream.forEach(A_DOUBLE_CONSUMER));
		test(stream -> stream.forEachOrdered(A_DOUBLE_CONSUMER));
		test(stream -> stream.max(), OptionalDouble.of(3));
		test(stream -> stream.min(), OptionalDouble.of(3));
		test(stream -> stream.noneMatch(A_DOUBLE_PREDICATE), true);
		test(stream -> stream.reduce(A_DOUBLE_BINARY_OPERATOR), OptionalDouble.of(3));
		test(stream -> stream.reduce(1, A_DOUBLE_BINARY_OPERATOR), 3d);
		test(DoubleStream::toArray, new double[1]);
		test(DoubleStream::sum, 1d);
		test(DoubleStream::average, OptionalDouble.of(3d));
		test(DoubleStream::summaryStatistics, new DoubleSummaryStatistics());
	}

	private static <T> void test(Consumer<DoubleStream> consumer) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return null;
			}

			@Override
			public T apply(DoubleStream stream) {
				consumer.accept(stream);
				return null;
			}
		});
	}

	private static <T> void test(Function<DoubleStream, T> function, T result) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return result;
			}

			@Override
			public T apply(DoubleStream stream) {
				return function.apply(stream);
			}
		});
	}

	private static <T extends BaseStream> void test(Class<? extends T> type, Function<DoubleStream, T> function) {
		INTERMEDIATE_OPERATIONS.add(new DoubleermediateOperation<T>() {
			@Override
			public Class<? extends T> type() {
				return type;
			}

			@Override
			public T apply(DoubleStream stream) {
				return function.apply(stream);
			}
		});
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DoubleStream delegate;
	private DoubleStream inTest;

	@Before
	public void setUp() {
		delegate = mock(DoubleStream.class);
		inTest = AutoClosingDoubleStream.from(delegate);
	}

	@Theory
	public void testIntermediateOperationReturnsNewAutoClosingStream(@FromDataPoints("intermediateOperations") DoubleermediateOperation intermediateOperation) {
		BaseStream newDelegate = (BaseStream) mock(intermediateOperation.type());
		when(intermediateOperation.apply(delegate)).thenReturn(newDelegate);

		BaseStream result = intermediateOperation.apply(inTest);

		assertThat(result, isAutoClosing());
		verifyDelegate(result, newDelegate);
	}

	@Theory
	public void testTerminalOperationDelegatesToAndClosesDelegate(@FromDataPoints("terminalOperations") TerminalOperation terminalOperation) {
		Object expectedResult = terminalOperation.result();
		if (expectedResult != null) {
			when(terminalOperation.apply(delegate)).thenReturn(expectedResult);
		}

		Object result = terminalOperation.apply(inTest);

		InOrder inOrder = inOrder(delegate);
		assertThat(result, is(expectedResult));
		inOrder.verify(delegate).close();
	}

	@Theory
	public void testTerminalOperationClosesDelegateEvenOnException(@FromDataPoints("terminalOperations") TerminalOperation terminalOperation) {
		RuntimeException exception = new RuntimeException();
		terminalOperation.apply(doThrow(exception).when(delegate));

		thrown.expect(is(exception));

		try {
			terminalOperation.apply(inTest);
		} finally {
			verify(delegate).close();
		}
	}

	private Matcher<BaseStream> isAutoClosing() {
		return is(anyOf(instanceOf(AutoClosingStream.class), instanceOf(AutoClosingDoubleStream.class), instanceOf(AutoClosingIntStream.class), instanceOf(AutoClosingLongStream.class)));
	}

	private void verifyDelegate(BaseStream result, BaseStream newDelegate) {
		result.close();
		verify(newDelegate).close();
	}

	private interface TerminalOperation<T> {

		T result();

		T apply(DoubleStream stream);

	}

	private interface DoubleermediateOperation<T extends BaseStream> {

		Class<? extends T> type();

		T apply(DoubleStream stream);

	}

}
