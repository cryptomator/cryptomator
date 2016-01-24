package org.cryptomator.common.test.mockito;

import static java.util.Arrays.asList;

import java.util.function.Consumer;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class Answers {

	public static <T> Answer<T> collectParameters(Answer<T> answer, Consumer<?>... parameterConsumers) {
		return new Answer<T>() {
			@SuppressWarnings({"rawtypes", "unchecked"})
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				for (int i = 0; i < invocation.getArguments().length; i++) {
					if (parameterConsumers.length > i) {
						((Consumer) parameterConsumers[i]).accept(invocation.getArguments()[i]);
					}
				}
				return answer.answer(invocation);
			}
		};

	}

	@SafeVarargs
	public static <T> Answer<T> consecutiveAnswers(Answer<T>... answers) {
		if (answers == null || answers.length == 0) {
			throw new IllegalArgumentException("Required at least one answer");
		}
		if (asList(answers).contains(null)) {
			throw new IllegalArgumentException("No answers must be null");
		}
		return new Answer<T>() {
			private int nextIndex = 0;

			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				try {
					return answers[nextIndex].answer(invocation);
				} finally {
					nextIndex = (nextIndex + 1) % answers.length;
				}
			}

		};
	}

	public static <T> Answer<T> value(T value) {
		return new Answer<T>() {
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				return value;
			}

		};
	}

}
