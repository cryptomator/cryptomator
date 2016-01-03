package org.cryptomator.filesystem.nio;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class ReflectiveClassMatchers {

	public static <T> Matcher<Class<T>> aClassThatDoesNotDeclareMethod(String name, Class<?>... parameterTypes) {
		return new TypeSafeDiagnosingMatcher<Class<T>>(Class.class) {
			@Override
			public void describeTo(Description description) {
				description //
						.appendText(" a Class that does not declare method ") //
						.appendText(name) //
						.appendValueList("(", ", ", ")", parameterTypes);
			}

			@Override
			protected boolean matchesSafely(Class<T> item, Description mismatchDescription) {
				if (declaredMethodToCheck(item)) {
					mismatchDescription //
							.appendText(" a Class that declares method ") //
							.appendText(name) //
							.appendValueList("(", ", ", ")", parameterTypes);
					return false;
				} else {
					return true;
				}
			}

			private boolean declaredMethodToCheck(Class<T> item) {
				try {
					return item.getDeclaredMethod(name, parameterTypes) != null;
				} catch (NoSuchMethodException e) {
					return false;
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	public static <T> Matcher<Class<T>> aClassThatDoesDeclareMethod(String name, Class<?>... parameterTypes) {
		return new TypeSafeDiagnosingMatcher<Class<T>>(Class.class) {
			@Override
			public void describeTo(Description description) {
				description //
						.appendText(" a Class that does declare method ") //
						.appendText(name) //
						.appendValueList("(", ", ", ")", parameterTypes);
			}

			@Override
			protected boolean matchesSafely(Class<T> item, Description mismatchDescription) {
				if (declaredMethodToCheck(item)) {
					return true;
				} else {
					mismatchDescription //
							.appendText(" a Class that does not declare method ") //
							.appendText(name) //
							.appendValueList("(", ", ", ")", parameterTypes);
					return false;
				}
			}

			private boolean declaredMethodToCheck(Class<T> item) {
				try {
					return item.getDeclaredMethod(name, parameterTypes) != null;
				} catch (NoSuchMethodException e) {
					return false;
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

}
