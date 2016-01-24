/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.common.test.matcher;

import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class PropertyMatcher<T, P> extends TypeSafeDiagnosingMatcher<T> {

	private final Class<T> expectedType;
	private final Function<? super T, P> getter;
	private final String name;
	private final Matcher<? super P> subMatcher;

	public PropertyMatcher(Class<T> type, Function<? super T, P> getter, String name, Matcher<? super P> subMatcher) {
		super(type);
		this.expectedType = type;
		this.getter = getter;
		this.name = name;
		this.subMatcher = subMatcher;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("a ") //
				.appendText(expectedType.getSimpleName()) //
				.appendText(" with a ") //
				.appendText(name) //
				.appendText(" that ") //
				.appendDescriptionOf(subMatcher);
	}

	@Override
	protected boolean matchesSafely(T item, Description mismatchDescription) {
		P propertyValue = getter.apply(item);
		if (subMatcher.matches(propertyValue)) {
			return true;
		} else {
			mismatchDescription.appendText("a ") //
					.appendText(expectedType.getSimpleName()) //
					.appendText(" with a ") //
					.appendText(name) //
					.appendText(" that ");
			subMatcher.describeMismatch(propertyValue, mismatchDescription);
			return false;
		}
	}

}
