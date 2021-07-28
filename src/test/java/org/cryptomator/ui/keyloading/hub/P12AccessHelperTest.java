package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class P12AccessHelperTest {

	@Test
	public void testCreate(@TempDir Path tmpDir) throws IOException {
		var p12File = tmpDir.resolve("test.p12");

		var keyPair = P12AccessHelper.createNew(p12File, "asd".toCharArray());

		Assertions.assertNotNull(keyPair);
		Assertions.assertTrue(Files.exists(p12File));
	}

	@Nested
	public class ExistingFile {

		private Path p12File;

		@BeforeEach
		public void setup(@TempDir Path tmpDir) throws IOException {
			p12File = tmpDir.resolve("test.p12");
			P12AccessHelper.createNew(p12File, "foo".toCharArray());
		}

		@Test
		public void testLoadWithWrongPassword() {
			Assertions.assertThrows(InvalidPassphraseException.class, () -> {
				P12AccessHelper.loadExisting(p12File, "bar".toCharArray());
			});
		}

		@Test
		public void testLoad() throws IOException {
			var keyPair = P12AccessHelper.loadExisting(p12File, "foo".toCharArray());

			Assertions.assertNotNull(keyPair);
		}
	}

}