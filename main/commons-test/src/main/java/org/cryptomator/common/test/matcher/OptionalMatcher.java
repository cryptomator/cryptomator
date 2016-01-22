package org.cryptomator.common.test.matcher;

import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class OptionalMatcher {

	public static <T> Matcher<Optional<T>> presentOptionalWithValueThat(Matcher<? super T> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Optional<T>>(Optional.class) {
			@Override
			public void describeTo(Description description) {
				description //
						.appendText("a present Optional with a value that ") //
						.appendDescriptionOf(valueMatcher);
			}

			@Override
			protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
				if (item.isPresent()) {
					if (valueMatcher.matches(item.get())) {
						return true;
					} else {
						mismatchDescription.appendText("a present Optional with value that ");
						valueMatcher.describeMismatch(item, mismatchDescription);
						return false;
					}
				} else {
					mismatchDescription.appendText("an empty Optional");
					return false;
				}

			}

		};
	}

	public static <T> Matcher<Optional<T>> emptyOptional() {
		return new TypeSafeDiagnosingMatcher<Optional<T>>(Optional.class) {
			@Override
			public void describeTo(Description description) {
				description.appendText("an empty Optional");
			}

			@Override
			protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
				if (item.isPresent()) {
					mismatchDescription.appendText("a present Optional of ").appendValue(item.get());
					return false;
				} else {
					return true;
				}

			}

		};
	}

}
