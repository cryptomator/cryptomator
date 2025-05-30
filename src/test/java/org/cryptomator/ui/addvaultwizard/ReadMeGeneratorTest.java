package org.cryptomator.ui.addvaultwizard;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

public class ReadMeGeneratorTest {

	@SuppressWarnings("SpellCheckingInspection")
	@ParameterizedTest
	@CsvSource({ //
			"test,test", //
			"t\u00E4st,t\\'E4st", //
			"t\uD83D\uDE09st,t\\uc1\\u55357\\uc1\\u56841st", //
	})
	public void testEscapeNonAsciiChars(String input, String expectedResult) {
		ReadmeGenerator readmeGenerator = new ReadmeGenerator(null);

		String actualResult = readmeGenerator.escapeNonAsciiChars(input);

		Assertions.assertEquals(expectedResult, actualResult);
	}

	@Test
	public void testCreateDocument() {
		ReadmeGenerator readmeGenerator = new ReadmeGenerator(null);
		Iterable<String> paragraphs = List.of( //
				"Dear User,", //
				"\\b please don't touch the \"d\" directory.", //
				"Thank you for your cooperation \uD83D\uDE09" //
		);

		String result = readmeGenerator.createDocument(paragraphs);

		MatcherAssert.assertThat(result, CoreMatchers.startsWith("{\\rtf1\\fbidis\\ansi\\uc0\\fs32"));
		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 Dear User,}\\par"));
		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 \\b please don't touch the \"d\" directory.}\\par "));
		MatcherAssert.assertThat(result, CoreMatchers.containsString("{\\sa80 Thank you for your cooperation \\uc1\\u55357\\uc1\\u56841}\\par"));
		MatcherAssert.assertThat(result, CoreMatchers.endsWith("}"));
	}

}
