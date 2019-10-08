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
	@DisplayName("validateRecoveryKey() with garbage input")
	public void testValidateValidateRecoveryKeyWithGarbageInput() {
		boolean result = inTest.validateRecoveryKey("löl");
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with too short input")
	public void testValidateValidateRecoveryKeyWithTooShortInput() {
		boolean result = inTest.validateRecoveryKey("them circumstances");
		Assertions.assertFalse(result);
	}

	@Test
	@DisplayName("validateRecoveryKey() with invalid crc32/16")
	public void testValidateValidateRecoveryKeyWithInvalidCrc() {
		boolean result = inTest.validateRecoveryKey("them circumstances conduct providing have gesture aged extraordinary generally silently" +
				" beasts rights sit country highest career wrought silently liberal altogether capacity David conscious word issue" +
				" ancient directed solitary how spain look smile see won't although dying obtain vol with c. asleep along listen circumstances");
		Assertions.assertFalse(result);
	}
	
	@Test
	@DisplayName("validateRecoveryKey() with valid key")
	public void testValidateValidateRecoveryKeyWithValidKey() {
		boolean result = inTest.validateRecoveryKey("them circumstances conduct providing have gesture aged extraordinary generally silently" +
				" beasts rights sit country highest career wrought silently liberal altogether capacity David conscious word issue" +
				" ancient directed solitary how spain look smile see won't although dying obtain vol with c. asleep along listen riding");
		Assertions.assertTrue(result);
	}

}