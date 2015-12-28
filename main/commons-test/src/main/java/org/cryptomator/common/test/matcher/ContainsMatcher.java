package org.cryptomator.common.test.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Wraps hamcrest contains and containsInAnyOrder matcher factory methods to
 * avoid problems due to incorrect / inconsistent handling of generics by
 * several java compilers.
 * 
 * @author Markus Kreusch
 */
public class ContainsMatcher {

	@SuppressWarnings({ "unchecked" })
	@SafeVarargs
	public static <T> Matcher<Iterable<? super T>> containsInAnyOrder(Matcher<? extends T>... matchers) {
		return Matchers.containsInAnyOrder((Matcher[]) matchers);
	}

	@SuppressWarnings({ "unchecked" })
	@SafeVarargs
	public static <T> Matcher<Iterable<? super T>> contains(Matcher<? extends T>... matchers) {
		return Matchers.contains((Matcher[]) matchers);
	}

}
