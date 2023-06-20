package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

public class PropertiesPreprocessorTest {

	@ParameterizedTest
	@CsvSource(value = """
			org.example.@{mytest1}.test, org.example.@{mytest1}.test
			@{only*word*digits*under_score\\},@{only*words*digits*under_score\\}
			C:\\Users\\@{appdir}\\dir, C:\\Users\\\\dir
			@{@{appdir}},@{}
			Longer @{appdir} text with @{appdir}., Longer  text with .
			""")
	public void test(String propertyValue, String expected) {
		var result = PropertiesPreprocessor.process(propertyValue);
		Assertions.assertEquals(result, expected);
	}

}
