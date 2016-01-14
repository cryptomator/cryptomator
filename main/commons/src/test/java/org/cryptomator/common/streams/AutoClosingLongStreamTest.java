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
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
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
public class AutoClosingLongStreamTest {

	private static final LongPredicate AN_LONG_PREDICATE = any -> true;
	private static final LongFunction AN_LONG_FUNCTION = i -> null;
	private static final BiConsumer A_BICONSUMER = (a, b) -> {
	};
	private static final Supplier A_SUPPLIER = () -> null;

	@DataPoints("intermediateOperations")
	public static final List<LongermediateOperation<?>> INTERMEDIATE_OPERATIONS = new ArrayList<>();

	@DataPoints("terminalOperations")
	public static final List<TerminalOperation<?>> TERMINAL_OPERATIONS = new ArrayList<>();
	private static final LongUnaryOperator AN_LONG_UNARY_OPERATOR = i -> 3;
	private static final LongToDoubleFunction AN_LONG_TO_DOUBLE_FUNCTION = i -> 3d;
	private static final LongToIntFunction AN_LONG_TO_INT_FUNCTION = i -> 5;
	private static final LongConsumer AN_LONG_CONSUMER = i -> {
	};
	private static final ObjLongConsumer AN_OBJ_LONG_CONSUMER = (a, b) -> {
	};
	private static final LongBinaryOperator AN_LONG_BINARY_OPERATOR = (a, b) -> a;

	static {
		// define intermediate operations
		test(LongStream.class, LongStream::distinct);
		test(LongStream.class, stream -> stream.filter(AN_LONG_PREDICATE));
		test(LongStream.class, stream -> stream.flatMap(AN_LONG_FUNCTION));
		test(LongStream.class, stream -> stream.limit(5));
		test(LongStream.class, stream -> stream.map(AN_LONG_UNARY_OPERATOR));
		test(DoubleStream.class, stream -> stream.mapToDouble(AN_LONG_TO_DOUBLE_FUNCTION));
		test(Stream.class, stream -> stream.mapToObj(AN_LONG_FUNCTION));
		test(IntStream.class, stream -> stream.mapToInt(AN_LONG_TO_INT_FUNCTION));
		test(LongStream.class, LongStream::parallel);
		test(LongStream.class, stream -> stream.peek(AN_LONG_CONSUMER));
		test(LongStream.class, LongStream::sequential);
		test(LongStream.class, stream -> stream.skip(5));
		test(LongStream.class, LongStream::sorted);
		test(LongStream.class, LongStream::unordered);
		test(Stream.class, LongStream::boxed);

		// define terminal operations
		test(stream -> stream.allMatch(AN_LONG_PREDICATE), true);
		test(stream -> stream.anyMatch(AN_LONG_PREDICATE), true);
		test(stream -> stream.collect(A_SUPPLIER, AN_OBJ_LONG_CONSUMER, A_BICONSUMER), 7L);
		test(LongStream::count, 3L);
		test(LongStream::findAny, OptionalLong.of(3));
		test(LongStream::findFirst, OptionalLong.of(3));
		test(stream -> stream.forEach(AN_LONG_CONSUMER));
		test(stream -> stream.forEachOrdered(AN_LONG_CONSUMER));
		test(stream -> stream.max(), OptionalLong.of(3));
		test(stream -> stream.min(), OptionalLong.of(3));
		test(stream -> stream.noneMatch(AN_LONG_PREDICATE), true);
		test(stream -> stream.reduce(AN_LONG_BINARY_OPERATOR), OptionalLong.of(3));
		test(stream -> stream.reduce(1, AN_LONG_BINARY_OPERATOR), 3L);
		test(LongStream::toArray, new long[1]);
		test(LongStream::sum, 1L);
		test(LongStream::average, OptionalDouble.of(3d));
		test(LongStream::summaryStatistics, new LongSummaryStatistics());
	}

	private static <T> void test(Consumer<LongStream> consumer) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return null;
			}

			@Override
			public T apply(LongStream stream) {
				consumer.accept(stream);
				return null;
			}
		});
	}

	private static <T> void test(Function<LongStream, T> function, T result) {
		TERMINAL_OPERATIONS.add(new TerminalOperation<T>() {
			@Override
			public T result() {
				return result;
			}

			@Override
			public T apply(LongStream stream) {
				return function.apply(stream);
			}
		});
	}

	private static <T extends BaseStream> void test(Class<? extends T> type, Function<LongStream, T> function) {
		INTERMEDIATE_OPERATIONS.add(new LongermediateOperation<T>() {
			@Override
			public Class<? extends T> type() {
				return type;
			}

			@Override
			public T apply(LongStream stream) {
				return function.apply(stream);
			}
		});
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private LongStream delegate;
	private LongStream inTest;

	@Before
	public void setUp() {
		delegate = mock(LongStream.class);
		inTest = AutoClosingLongStream.from(delegate);
	}

	@Theory
	public void testIntermediateOperationReturnsNewAutoClosingStream(@FromDataPoints("intermediateOperations") LongermediateOperation intermediateOperation) {
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

		T apply(LongStream stream);

	}

	private interface LongermediateOperation<T extends BaseStream> {

		Class<? extends T> type();

		T apply(LongStream stream);

	}

}
