package org.cryptomator.ui.addvaultwizard;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ReadMeGeneratorTest {

	// ------------------------------------------------------------------------
	// 1. Tests for escapeNonAsciiChars(CharSequence)
	// ------------------------------------------------------------------------


	@Test
	void testEscapeNonAsciiChars_asciiOnly() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		String input = "test";
		String result = generator.escapeNonAsciiChars(input);
		// Should remain identical
		Assertions.assertEquals("test", result);
	}


	@Test
	void testEscapeNonAsciiChars_singleUmlaut() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		String input = "täst"; // "ä" is U+00E4
		String result = generator.escapeNonAsciiChars(input);
		Assertions.assertEquals("t\\u228st", result);
	}


	@Test
	void testEscapeNonAsciiChars_emoji() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		// "😉"
		String input = "t😉st";
		String result = generator.escapeNonAsciiChars(input);


		Assertions.assertTrue(result.contains("\\u55357"),
				"Should contain \\u55357 for the high surrogate of 😉");
		Assertions.assertTrue(result.contains("\\u56841"),
				"Should contain \\u56841 for the low surrogate of 😉");
		// The rest of the string "t" and "st" remain unchanged.
		Assertions.assertTrue(result.startsWith("t"), "Should start with t");
		Assertions.assertTrue(result.endsWith("st"), "Should end with st");
	}


	@Test
	void testEscapeNonAsciiChars_mixed() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		String input = "héllô 😉";
		String result = generator.escapeNonAsciiChars(input);

		Assertions.assertTrue(result.contains("\\u233"), "Should contain \\u233 for 'é'");
		Assertions.assertTrue(result.contains("\\u244"), "Should contain \\u244 for 'ô'");
		Assertions.assertTrue(result.contains("\\u55357"), "Should contain \\u55357 for 😉");
		Assertions.assertTrue(result.contains("\\u56841"), "Should contain \\u56841 for 😉");
	}


	@Test
	void testEscapeNonAsciiChars_emptyString() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		String input = "";
		String result = generator.escapeNonAsciiChars(input);
		Assertions.assertTrue(result.isEmpty(), "Escaped empty string should be empty");
	}

	// ------------------------------------------------------------------------
	// 2. Tests for createDocument(Iterable<String>)
	// ------------------------------------------------------------------------

	@Test
	void testCreateDocument_emptyParagraphs() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		List<String> paragraphs = Collections.emptyList();

		String result = generator.createDocument(paragraphs);

		// RTF header:
		MatcherAssert.assertThat(result, CoreMatchers.startsWith("{\\rtf1\\fbidis\\ansi\\uc0\\fs32"));
		// RTF footer:
		MatcherAssert.assertThat(result, CoreMatchers.endsWith("}"));
		// No paragraphs => no "{\\sa80" tags
		MatcherAssert.assertThat(result, CoreMatchers.not(CoreMatchers.containsString("\\sa80")));
	}


	@Test
	void testCreateDocument_singleParagraph() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		List<String> paragraphs = List.of("Hello World");

		String result = generator.createDocument(paragraphs);

		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 Hello World}\\par"));
	}


	@Test
	void testCreateDocument_multipleParagraphs() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		List<String> paragraphs = List.of("Paragraph One", "Paragraph Two");

		String result = generator.createDocument(paragraphs);

		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 Paragraph One}\\par"));
		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 Paragraph Two}\\par"));
	}


	@Test
	void testCreateDocument_nonAsciiParagraph() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		List<String> paragraphs = List.of("Hällo");

		String result = generator.createDocument(paragraphs);

		MatcherAssert.assertThat(result, CoreMatchers.containsString("H\\u228llo"));
	}


	@Test
	void testCreateDocument_specialRtfCharacters() {
		ReadmeGenerator generator = new ReadmeGenerator(null);
		List<String> paragraphs = List.of("\\b Bold text");

		String result = generator.createDocument(paragraphs);

		// Expect the literal "\b" to appear.
		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 \\b Bold text}\\par"));
	}

	// ------------------------------------------------------------------------
	// 3. Tests for createVaultStorageLocationReadmeRtf() / createVaultAccessLocationReadmeRtf()
	//    using a mocked ResourceBundle
	// ------------------------------------------------------------------------


	@Test
	void testCreateVaultStorageLocationReadmeRtf_allKeysPresent() {
		ResourceBundle mockBundle = mock(ResourceBundle.class);

		// The method references .1 through .10
		when(mockBundle.getString("addvault.new.readme.storageLocation.1"))
				.thenReturn("Storage Header ä");
		when(mockBundle.getString("addvault.new.readme.storageLocation.2"))
				.thenReturn("Line 2");
		when(mockBundle.getString("addvault.new.readme.storageLocation.3"))
				.thenReturn("Line 3");
		when(mockBundle.getString("addvault.new.readme.storageLocation.4"))
				.thenReturn("Line 4");
		when(mockBundle.getString("addvault.new.readme.storageLocation.5"))
				.thenReturn("Line 5");
		when(mockBundle.getString("addvault.new.readme.storageLocation.6"))
				.thenReturn("Line 6");
		when(mockBundle.getString("addvault.new.readme.storageLocation.7"))
				.thenReturn("Line 7");
		when(mockBundle.getString("addvault.new.readme.storageLocation.8"))
				.thenReturn("Line 8");
		when(mockBundle.getString("addvault.new.readme.storageLocation.9"))
				.thenReturn("Line 9");
		// The final line references HELP_URL with %s
		when(mockBundle.getString("addvault.new.readme.storageLocation.10"))
				.thenReturn("Please visit %s for help.");

		ReadmeGenerator generator = new ReadmeGenerator(mockBundle);
		String result = generator.createVaultStorageLocationReadmeRtf();


		Assertions.assertTrue(result.contains("Storage Header"));
		Assertions.assertTrue(result.contains("\\u228"), "Should contain \\u228 for 'ä'");
		// The help URL is appended
		Assertions.assertTrue(result.contains("http://docs.cryptomator.org"),
				"Should include the docs link from HELP_URL");
	}


	@Test
	void testCreateVaultStorageLocationReadmeRtf_missingKey() {
		ResourceBundle mockBundle = mock(ResourceBundle.class);
		// Return only for the first key, missing others
		when(mockBundle.getString(anyString())).thenAnswer(invocation -> {
			String key = invocation.getArgument(0);
			if ("addvault.new.readme.storageLocation.1".equals(key)) {
				return "Header line";
			}
			throw new MissingResourceException("Missing key", "TestBundle", key);
		});

		ReadmeGenerator generator = new ReadmeGenerator(mockBundle);
		Assertions.assertThrows(MissingResourceException.class,
				generator::createVaultStorageLocationReadmeRtf);
	}


	@Test
	void testCreateVaultAccessLocationReadmeRtf_allKeysPresent() {
		ResourceBundle mockBundle = mock(ResourceBundle.class);
		when(mockBundle.getString("addvault.new.readme.accessLocation.1"))
				.thenReturn("Access Header ñ");
		when(mockBundle.getString("addvault.new.readme.accessLocation.2"))
				.thenReturn("Access line 2");
		when(mockBundle.getString("addvault.new.readme.accessLocation.3"))
				.thenReturn("Access line 3");
		when(mockBundle.getString("addvault.new.readme.accessLocation.4"))
				.thenReturn("Access line 4");

		ReadmeGenerator generator = new ReadmeGenerator(mockBundle);
		String result = generator.createVaultAccessLocationReadmeRtf();


		Assertions.assertTrue(result.contains("Access Header"));
		Assertions.assertTrue(result.contains("\\u241"),
				"Should contain \\u241 for 'ñ'");
		Assertions.assertTrue(result.contains("Access line 2"));
		Assertions.assertTrue(result.contains("Access line 3"));
		Assertions.assertTrue(result.contains("Access line 4"));
	}


	@Test
	void testCreateVaultAccessLocationReadmeRtf_missingKey() {
		ResourceBundle mockBundle = mock(ResourceBundle.class);
		// Return only for "accessLocation.1", missing others
		when(mockBundle.getString(anyString())).thenAnswer(invocation -> {
			String key = invocation.getArgument(0);
			if ("addvault.new.readme.accessLocation.1".equals(key)) {
				return "Access Header";
			}
			throw new MissingResourceException("Missing key", "TestBundle", key);
		});

		ReadmeGenerator generator = new ReadmeGenerator(mockBundle);
		Assertions.assertThrows(MissingResourceException.class,
				generator::createVaultAccessLocationReadmeRtf);
	}

}
