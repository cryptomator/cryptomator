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
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
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
public class AutoClosingIntStreamTest {

	private static final IntPredicate AN_INT_PREDICATE = any -> true;
	private static final IntFunction AN_INT_FUNCTION = i -> null;
	private static final BiConsumer A_BICONSUMER = (a, b) -> {
	};
	private static final Supplier A_SUPPLIER = () -> null;

	@DataPoints("intermediateOperations")
	public static final List<IntermediateOperation<?>> INTERMEDIATE_OPERATIONS = new ArrayList<>();

	@DataPoints("terminalOperations")
	public static final List<TerminalOperation<?>> TERMINAL_OPERATIONS = new ArrayList<>();
	private static final IntUnaryOperator AN_INT_UNARY_OPERATOR = i -> 3;
	private static final IntToDoubleFunction AN_INT_TO_DOUBLE_FUNCTION = i -> 3d;
	private static final IntToLongFunction AN_INT_TO_LONG_FUNCTION = i -> 5L;
	private static final IntConsumer AN_INT_CONSUMER = i -> {
	};
	private static final ObjIntConsumer AN_OBJ_INT_CONSUMER = (a, b) -> {
	};
	private static final IntBinaryOperator AN_INT_BINARY_OPERATOR = (a, b) -> a;

	static {
		// define intermediate operations
		test(IntStream.class, IntStream::distinct);
		test(IntStream.class, stream -> stream.filter(AN_INT_PREDICATE));
		test(IntStream.class, stream -> stream.flatMap(AN_INT_FUNCTION));
		test(IntStream.class, stream -> stream.limit(5));
		test(IntStream.class, stream -> stream.map(AN_INT_UNARY_OPERATOR));
		test(DoubleStream.class, stream -> stream.mapToDouble(AN_INT_TO_DOUBLE_FUNCTION));
		test(Stream.class, stream -> stream.mapToObj(AN_INT_FUNCTION));
		test(LongStream.class, stream -> stream.mapToLong(AN_INT_TO_LONG_FUNCTION));
		test(IntStream.class, IntStream::parallel);
		test(IntStream.class, stream -> stream.peek(AN_INT_CONSUMER));
		test(IntStream.class, IntStream::sequential);
		test(IntStream.class, stream -> stream.skip(5));
		test(IntStream.class, IntStream::sorted);
		test(IntStream.class, IntStream::unordered);
		test(Stream.class, IntStream::boxed);

		// define terminal operations
		test(stream -> stream.allMatch(AN_INT_PREDICATE), true);
		test(stream -> stream.anyMatch(AN_INT_PREDICATE), true);
		test(stream -> stream.collect(A_SUPPLIER, AN_OBJ_INT_CONSUMER, A_BICONSUMER), 7);
		test(IntStream::count, 3L);
		test(IntStream::findAny, OptionalInt.of(3));
		test(IntStream::findFirst, OptionalInt.of(3));
		test(stream -> stream.forEach(AN_INT_CONSUMER));
		test(stream -> stream.forEachOrdered(AN_INT_CONSUMER));
		test(stream -> stream.max(), OptionalInt.of(3));
		test(stream -> stream.min(), OptionalInt.of(3));
		test(stream -> stream.noneMatch(AN_INT_PREDICATE), true);
		test(stream -> stream.reduce(AN_INT_BINARY_OPERATOR), OptionalInt.of(3));
		test(stream -> stream.reduce(1, AN_INT_BINARY_OPERATOR), 3);
		test(IntStream::toArray, new int[1]);
		test(IntStream::sum, 1);
		test(IntStream::average, OptionalDouble.of(3d));
		test(IntStream::summaryStatistics, new IntSummaryStatistics());
	}

	private static <T> void test(Consumer<IntStream> consumer) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return null;
			}

			@Override
			public T apply(IntStream stream) {
				consumer.accept(stream);
				return null;
			}
		});
	}

	private static <T> void test(Function<IntStream, T> function, T result) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return result;
			}

			@Override
			public T apply(IntStream stream) {
				return function.apply(stream);
			}
		});
	}

	private static <T extends BaseStream> void test(Class<? extends T> type, Function<IntStream, T> function) {
		INTERMEDIATE_OPERATIONS.add(new IntermediateOperation<T>() {
			@Override
			public Class<? extends T> type() {
				return type;
			}

			@Override
			public T apply(IntStream stream) {
				return function.apply(stream);
			}
		});
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IntStream delegate;
	private IntStream inTest;

	@Before
	public void setUp() {
		delegate = mock(IntStream.class);
		inTest = AutoClosingIntStream.from(delegate);
	}

	@Theory
	public void testIntermediateOperationReturnsNewAutoClosingStream(@FromDataPoints("intermediateOperations") IntermediateOperation intermediateOperation) {
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

		T apply(IntStream stream);

	}

	private interface IntermediateOperation<T extends BaseStream> {

		Class<? extends T> type();

		T apply(IntStream stream);

	}

}
