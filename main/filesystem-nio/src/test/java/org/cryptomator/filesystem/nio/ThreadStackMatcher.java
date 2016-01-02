package org.cryptomator.filesystem.nio;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

class ThreadStackMatcher extends TypeSafeDiagnosingMatcher<Thread> {

	public static Matcher<Thread> stackContains(Class<?> type, String methodName) {
		return new ThreadStackMatcher(type, methodName);
	}

	private final Class<?> type;
	private final String methodName;

	private ThreadStackMatcher(Class<?> type, String methodName) {
		super(Thread.class);
		this.type = type;
		this.methodName = methodName;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a thread executing ") //
				.appendValue(type) //
				.appendText(".") //
				.appendText(methodName);
	}

	@Override
	protected boolean matchesSafely(Thread item, Description mismatchDescription) {
		StackTraceElement[] stack = item.getStackTrace();
		if (!containsNeededStackTraceElement(stack)) {
			mismatchDescription.appendText("a thread not executing ") //
					.appendValue(type) //
					.appendText(".") //
					.appendText(methodName);
			return false;
		} else {
			return true;
		}
	}

	private boolean containsNeededStackTraceElement(StackTraceElement[] stack) {
		for (StackTraceElement stackTraceElement : stack) {
			if (isNeededStackTraceElement(stackTraceElement)) {
				return true;
			}
		}
		return false;
	}

	private boolean isNeededStackTraceElement(StackTraceElement stackTraceElement) {
		return type.getName().equals(stackTraceElement.getClassName()) //
				&& methodName.equals(stackTraceElement.getMethodName());
	}

}
