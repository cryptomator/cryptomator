package org.cryptomator.commons.test.matcher;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Wraps hamcrest contains and containsInAny order matcher factory methods to
 * avoid problems due to incorrect / inconsistent handling of generics by the
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
