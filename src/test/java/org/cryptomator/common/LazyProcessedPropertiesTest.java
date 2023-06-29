package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

public class LazyProcessedPropertiesTest {

	@ParameterizedTest
	@CsvSource(value = {"org.example.@{mytest1}.test, org.example.@{mytest1}.test", //
			"@{only*words*digits*under_score\\},@{only*words*digits*under_score\\}", //
			"C:\\Users\\@{appdir}\\dir, C:\\Users\\foobar\\dir", //
			"@{@{appdir}},@{foobar}", //
			"Longer @{appdir} text with @{appdir}., Longer foobar text with foobar."})
	public void test(String propertyValue, String expected) {
		LazyProcessedProperties inTest = new LazyProcessedProperties(System.getProperties(), Map.of("APPDIR", "foobar"));
		var result = inTest.process(propertyValue);
		Assertions.assertEquals(result, expected);
	}

}
