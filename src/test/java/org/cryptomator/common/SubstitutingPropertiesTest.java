package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Properties;

public class SubstitutingPropertiesTest {

	SubstitutingProperties inTest;

	@Nested
	public class Processing {

		@ParameterizedTest
		@DisplayName("Test template replacement")
		@CsvSource(textBlock = """
				unknown.@{testToken}.test, unknown.@{testToken}.test
				@{only*words*digits*under_score},@{only*words*digits*under_score}
				C:\\Users\\@{appdir}\\dir, C:\\Users\\foobar\\dir
				@{@{appdir}},@{foobar}
				Replacing several @{appdir} with @{appdir}., Replacing several foobar with foobar.""")
		public void test(String propertyValue, String expected) {
			SubstitutingProperties inTest = new SubstitutingProperties(Mockito.mock(Properties.class), Map.of("APPDIR", "foobar"));
			var result = inTest.process(propertyValue);
			Assertions.assertEquals(expected, result);
		}

		@Test
		@DisplayName("@{userhome} is replaced with the user home directory")
		public void testPropSubstitutions() {
			var props = new Properties();
			props.setProperty("user.home", "OneUponABit");

			inTest = new SubstitutingProperties(props, Map.of());
			var result = inTest.process("@{userhome}");
			Assertions.assertEquals("OneUponABit", result);
		}

		@DisplayName("Other keywords are replaced accordingly")
		@ParameterizedTest(name = "Token \"{0}\" replaced with content of {1}")
		@CsvSource(value = {"appdir, APPDIR, foobar", "appdata, APPDATA, bazbaz", "localappdata, LOCALAPPDATA, boboAlice"})
		public void testEnvSubstitutions(String token, String envName, String expected) {
			inTest = new SubstitutingProperties(new Properties(), Map.of(envName, expected));
			var result = inTest.process("@{" + token + "}");
			Assertions.assertEquals(expected, result);
		}

	}


	@Nested
	public class GetProperty {

		@Test
		@DisplayName("Undefined properties are not processed")
		public void testNoProcessingOnNull() {
			inTest = Mockito.spy(new SubstitutingProperties(new Properties(), Map.of()));

			var result = inTest.getProperty("some.prop");
			Assertions.assertNull(result);
			Mockito.verify(inTest, Mockito.never()).process(Mockito.anyString());
		}

		@ParameterizedTest
		@DisplayName("Properties not starting with \"cryptomator.\" are not processed")
		@ValueSource(strings = {"example.foo", "cryptomatorSomething.foo", "org.cryptomator.foo", "cryPtoMAtor.foo"})
		public void testNoProcessingOnNotCryptomator(String propKey) {
			var props = new Properties();
			props.setProperty(propKey, "someValue");
			inTest = Mockito.spy(new SubstitutingProperties(props, Map.of()));

			var result = inTest.getProperty("some.prop");
			Assertions.assertNull(result);
			Mockito.verify(inTest, Mockito.never()).process(Mockito.anyString());
		}

		@Test
		@DisplayName("Non-null property starting with \"cryptomator.\" is processed")
		public void testProcessing() {
			var props = new Properties();
			props.setProperty("cryptomator.prop", "someValue");
			inTest = Mockito.spy(new SubstitutingProperties(props, Map.of()));
			Mockito.doReturn("someValue").when(inTest).process(Mockito.anyString());

			inTest.getProperty("cryptomator.prop");
			Mockito.verify(inTest).process("someValue");
		}

		@Test
		@DisplayName("Default value is not processed")
		public void testNoProcessingDefault() {
			var props = Mockito.mock(Properties.class);
			Mockito.when(props.getProperty("cryptomator.prop")).thenReturn(null);
			inTest = Mockito.spy(new SubstitutingProperties(props, Map.of()));
			Mockito.doReturn("someValue").when(inTest).process(Mockito.anyString());

			var result = inTest.getProperty("cryptomator.prop", "a default");
			Assertions.assertEquals("a default", result);
			Mockito.verify(inTest, Mockito.never()).process(Mockito.any());
		}
	}

	@ParameterizedTest(name = "{0}={1} -> {0}={2}")
	@DisplayName("Replace @{userhome} during getProperty()")
	@CsvSource(quoteCharacter = '"', textBlock = """
			cryptomator.settingsPath, "@{userhome}/.config/Cryptomator/settings.json:@{userhome}/.Cryptomator/settings.json", "/home/.config/Cryptomator/settings.json:/home/.Cryptomator/settings.json"
			cryptomator.ipcSocketPath, "@{userhome}/.config/Cryptomator/ipc.socket:@{userhome}/.Cryptomator/ipc.socket", "/home/.config/Cryptomator/ipc.socket:/home/.Cryptomator/ipc.socket"
			not.cryptomator.not.substituted, "@{userhome}/foo", "@{userhome}/foo"
			cryptomator.no.placeholder.found, "foo/bar", "foo/bar"
			""")
	public void testEndToEndPropsSource(String key, String raw, String substituted) {
		var delegate = Mockito.mock(Properties.class);
		Mockito.doReturn("/home").when(delegate).getProperty("user.home");
		Mockito.doReturn(raw).when(delegate).getProperty(key);
		var inTest = new SubstitutingProperties(delegate, Map.of());

		var result = inTest.getProperty(key);

		Assertions.assertEquals(substituted, result);
	}

	@ParameterizedTest(name = "{0}={1} -> {0}={2}")
	@DisplayName("Replace appdata,localappdata or appdir during getProperty()")
	@CsvSource(quoteCharacter = '"', textBlock = """
			cryptomator.settingsPath, "@{appdata}/Cryptomator/settings.json", "C:\\Users\\JimFang\\AppData\\Roaming/Cryptomator/settings.json"
			cryptomator.ipcSocketPath, "@{localappdata}/Cryptomator/ipc.socket", "C:\\Users\\JimFang\\AppData\\Local/Cryptomator/ipc.socket"
			cryptomator.integrationsLinux.trayIconsDir, "@{appdir}/hicolor", "/squashfs1337/usr/hicolor"
			not.cryptomator.not.substituted, "@{appdir}/foo", "@{appdir}/foo"
			cryptomator.no.placeholder.found, "foo/bar", "foo/bar"
			""")
	public void testEndToEndEnvSource(String key, String raw, String substituted) {
		var delegate = Mockito.mock(Properties.class);
		Mockito.doReturn(raw).when(delegate).getProperty(key);
		var env = Map.of("APPDATA", "C:\\Users\\JimFang\\AppData\\Roaming", //
				"LOCALAPPDATA", "C:\\Users\\JimFang\\AppData\\Local", //
				"APPDIR", "/squashfs1337/usr");
		var inTest = new SubstitutingProperties(delegate, env);

		var result = inTest.getProperty(key);

		Assertions.assertEquals(substituted, result);
	}
}
