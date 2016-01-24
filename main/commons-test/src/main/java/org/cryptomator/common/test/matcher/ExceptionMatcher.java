package org.cryptomator.common.test.matcher;

import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class ExceptionMatcher<T extends Throwable> extends TypeSafeDiagnosingMatcher<T> {

	public static <T extends Throwable> ExceptionMatcher<T> ofType(Class<T> exceptionType) {
		return new ExceptionMatcher<>(exceptionType);
	}

	private final Class<T> exceptionType;
	private final Optional<Matcher<T>> subMatcher;

	private ExceptionMatcher(Class<T> exceptionType) {
		super(exceptionType);
		this.exceptionType = exceptionType;
		this.subMatcher = Optional.empty();
	}

	private ExceptionMatcher(Class<T> exceptionType, Matcher<T> subMatcher) {
		super(exceptionType);
		this.exceptionType = exceptionType;
		this.subMatcher = Optional.of(subMatcher);
	}

	@Override
	public void describeTo(Description description) {
		subMatcher.ifPresent(description::appendDescriptionOf);
	}

	@Override
	protected boolean matchesSafely(T item, Description mismatchDescription) {
		if (subMatcher.map(matcher -> !matcher.matches(item)).orElse(false)) {
			subMatcher.get().describeMismatch(item, mismatchDescription);
			return false;
		}
		return true;
	}

	public Matcher<T> withCauseThat(Matcher<? super Throwable> matcher) {
		return new ExceptionMatcher<T>(exceptionType, new PropertyMatcher<>(exceptionType, Throwable::getCause, "cause", matcher));
	}

}
