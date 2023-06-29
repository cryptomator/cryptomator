package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

public class LazyProcessedPropertiesTest {

	LazyProcessedProperties inTest;
	@ParameterizedTest
	@DisplayName("Test template replacement")
	@CsvSource(value = {"unknown.@{testToken}.test, unknown.@{testToken}.test", //
			"@{only*words*digits*under_score\\},@{only*words*digits*under_score\\}", //
			"C:\\Users\\@{appdir}\\dir, C:\\Users\\foobar\\dir", //
			"@{@{appdir}},@{foobar}", //
			"Longer @{appdir} text with @{appdir}., Longer foobar text with foobar."})
	public void test(String propertyValue, String expected) {
		LazyProcessedProperties inTest = new LazyProcessedProperties(System.getProperties(), Map.of("APPDIR", "foobar"));
		var result = inTest.process(propertyValue);
		Assertions.assertEquals(result, expected);
	}

	@Test
	@DisplayName("@{userhome} is replaced with the user home directory")
	public void testUserhome(){
		var expected = System.getProperty("user.home");

		inTest = new LazyProcessedProperties(System.getProperties(), Map.of());
		var result = inTest.process("@{userhome}");
		Assertions.assertEquals(result, expected);
	}

	@DisplayName("Other keywords are replaced accordingly")
	@ParameterizedTest(name = "Token \"{0}\" replaced with content of {1}")
	@CsvSource(value = {"appdir, APPDIR, foobar",
						"appdata, APPDATA, bazbaz",
						"localappdata, LOCALAPPDATA, boboAlice"})
	public void testAppDir(String token, String envName, String expected){
		inTest = new LazyProcessedProperties(System.getProperties(), Map.of(envName, expected));
		var result = inTest.process("@{"+token+"}");
		Assertions.assertEquals(result, expected);
	}

}
