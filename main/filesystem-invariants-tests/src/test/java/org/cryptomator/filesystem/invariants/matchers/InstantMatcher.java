package org.cryptomator.filesystem.invariants.matchers;

import static java.lang.String.format;

import java.time.Instant;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class InstantMatcher {

	public static Matcher<Instant> inRangeInclusiveWithTolerance(Instant min, Instant max, int toleranceInMilliseconds) {
		return inRangeInclusive(min.minusMillis(toleranceInMilliseconds), max.plusMillis(toleranceInMilliseconds));
	}

	public static Matcher<Instant> inRangeInclusive(Instant min, Instant max) {
		return new TypeSafeDiagnosingMatcher<Instant>(Instant.class) {
			@Override
			public void describeTo(Description description) {
				description.appendText(format("an Instant in [%s,%s]", min, max));
			}

			@Override
			protected boolean matchesSafely(Instant item, Description mismatchDescription) {
				if (item.isBefore(min) || item.isAfter(max)) {
					mismatchDescription.appendText("the instant ").appendValue(item);
					return false;
				}
				return true;
			}
		};
	}

}
