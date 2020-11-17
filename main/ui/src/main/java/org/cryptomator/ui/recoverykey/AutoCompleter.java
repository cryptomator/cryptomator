package org.cryptomator.ui.recoverykey;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AutoCompleter {

	private final List<String> dictionary;

	public AutoCompleter(Collection<String> dictionary) {
		this.dictionary = unmodifiableSortedRandomAccessList(dictionary);
	}

	private static <T extends Comparable<T>> List<T> unmodifiableSortedRandomAccessList(Collection<T> items) {
		List<T> result = new ArrayList<>(items);
		Collections.sort(result);
		return Collections.unmodifiableList(result);
	}

	public Optional<String> autocomplete(String prefix) {
		if (Strings.isNullOrEmpty(prefix)) {
			return Optional.empty();
		}
		int potentialMatchIdx = findIndexOfLexicographicallyPreceeding(0, dictionary.size(), prefix);
		if (potentialMatchIdx < dictionary.size()) {
			String potentialMatch = dictionary.get(potentialMatchIdx);
			return potentialMatch.startsWith(prefix) ? Optional.of(potentialMatch) : Optional.empty();
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Find the index of the first word in {@link #dictionary} that starts with a given prefix.
	 * <p>
	 * This method performs an "unsuccessful" binary search (it doesn't return when encountering an exact match).
	 * Instead it continues searching in the left half (which includes the exact match) until only one element is left.
	 * <p>
	 * If the dictionary doesn't contain a word "left" of the given prefix, this method returns an invalid index, though.
	 *
	 * @param begin Index of first element (inclusive)
	 * @param end Index of last element (exclusive)
	 * @param prefix
	 * @return index between [0, dictLen], i.e. index can exceed the upper bounds of {@link #dictionary}.
	 */
	private int findIndexOfLexicographicallyPreceeding(int begin, int end, String prefix) {
		if (begin >= end) {
			return begin; // this is usually where a binary search ends "unsuccessful"
		}

		int mid = (begin + end) / 2;
		String word = dictionary.get(mid);
		if (prefix.compareTo(word) <= 0) { // prefix preceeds or matches word
			// proceed in left half
			assert mid < end;
			return findIndexOfLexicographicallyPreceeding(0, mid, prefix);
		} else {
			// proceed in right half
			assert mid >= begin;
			return findIndexOfLexicographicallyPreceeding(mid + 1, end, prefix);
		}
	}

}
