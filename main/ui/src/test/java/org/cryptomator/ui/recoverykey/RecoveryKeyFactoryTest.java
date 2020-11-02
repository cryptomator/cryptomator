package org.cryptomator.ui.recoverykey;

import com.google.common.base.Splitter;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

class RecoveryKeyFactoryTest {

	private WordEncoder wordEncoder = new WordEncoder();
	private RecoveryKeyFactory inTest = new RecoveryKeyFactory(wordEncoder);

	@Test
	@DisplayName("createRecoveryKey() creates 44 words")
	public void testCreateRecoveryKey(@TempDir Path pathToVault) throws IOException {
		CryptoFileSystemProvider.initialize(pathToVault, "masterkey.cryptomator", "asd");
		String recoveryKey = inTest.createRecoveryKey(pathToVault, "asd");
		Assertions.assertNotNull(recoveryKey);
		Assertions.assertEquals(44, Splitter.on(' ').splitToList(recoveryKey).size()); // 66 bytes encoded as 44 words
	}

	@Test
	@DisplayName("validateRecoveryKey() with odd number of words")
	public void testValidateValidateRecoveryKeyWithOddNumberOfWords() {
		boolean result = inTest.validateRecoveryKey("pathway");
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with words not in dictionary")
	public void testValidateValidateRecoveryKeyWithGarbageInput() {
		boolean result = inTest.validateRecoveryKey("Backpfeifengesicht Schweinehund"); // according to le internet these are typical German words
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with too short input")
	public void testValidateValidateRecoveryKeyWithTooShortInput() {
		boolean result = inTest.validateRecoveryKey("pathway lift");
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with invalid checksum")
	public void testValidateValidateRecoveryKeyWithInvalidCrc() {
		boolean result = inTest.validateRecoveryKey("pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe" //
				+ " charity border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed investor boot" //
				+ " depth left theory snow whereby terminal weekly reject happiness circuit partial cup wrong");
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with valid input")
	public void testValidateValidateRecoveryKeyWithValidKey() {
		boolean result = inTest.validateRecoveryKey("pathway lift abuse plenty export texture gentleman landscape beyond ceiling around leaf cafe" //
				+ " charity border breakdown victory surely computer cat linger restrict infer crowd live computer true written amazed investor boot" //
				+ " depth left theory snow whereby terminal weekly reject happiness circuit partial cup ad");
		Assertions.assertTrue(result);
	}

}