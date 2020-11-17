package org.cryptomator.ui.recoverykey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Set;

class AutoCompleterTest {

	@Test
	@DisplayName("no match in []")
	public void testNoMatchInEmptyDict() {
		AutoCompleter autoCompleter = new AutoCompleter(Set.of());
		Optional<String> result = autoCompleter.autocomplete("tea");
		Assertions.assertFalse(result.isPresent());
	}

	@Test
	@DisplayName("no match for \"\"")
	public void testNoMatchForEmptyString() {
		AutoCompleter autoCompleter = new AutoCompleter(Set.of("asd"));
		Optional<String> result = autoCompleter.autocomplete("");
		Assertions.assertFalse(result.isPresent());
	}

	@Nested
	@DisplayName("search in dict: ['tame', 'teach', 'teacher']")
	class NarrowedDownDict {

		AutoCompleter autoCompleter = new AutoCompleter(Set.of("tame", "teach", "teacher"));

		@ParameterizedTest
		@DisplayName("find 'tame'")
		@ValueSource(strings = {"t", "ta", "tam", "tame"})
		public void testFindTame(String prefix) {
			Optional<String> result = autoCompleter.autocomplete(prefix);
			Assertions.assertTrue(result.isPresent());
			Assertions.assertEquals("tame", result.get());
		}

		@ParameterizedTest
		@DisplayName("find 'teach'")
		@ValueSource(strings = {"te", "tea", "teac", "teach"})
		public void testFindTeach(String prefix) {
			Optional<String> result = autoCompleter.autocomplete(prefix);
			Assertions.assertTrue(result.isPresent());
			Assertions.assertEquals("teach", result.get());
		}

		@ParameterizedTest
		@DisplayName("find 'teacher'")
		@ValueSource(strings = {"teache", "teacher"})
		public void testFindTeacher(String prefix) {
			Optional<String> result = autoCompleter.autocomplete(prefix);
			Assertions.assertTrue(result.isPresent());
			Assertions.assertEquals("teacher", result.get());
		}

		@Test
		@DisplayName("don't find 'teachers'")
		public void testDontFindTeachers() {
			Optional<String> result = autoCompleter.autocomplete("teachers");
			Assertions.assertFalse(result.isPresent());
		}

	}

}