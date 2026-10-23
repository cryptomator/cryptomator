package org.cryptomator.common;

import org.cryptomator.cryptolib.common.MasterkeyFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CryptomatorMKFValidatorTest {

	@ParameterizedTest(name = "N={0}, r={1}")
	@CsvSource({"32768, 8", "65536, 8"})
	public void testAcceptsParametersWithinLimit(int costParam, int blockSize) {
		var file = masterkeyFile(costParam, blockSize);

		Assertions.assertDoesNotThrow(() -> CryptomatorMKFValidator.INSTANCE.validate(file));
	}

	@ParameterizedTest(name = "N={0}, r={1}")
	@CsvSource({"131072, 8", "65536, 16", "1048576, 8", "1024, 128"}) // last one is within the memory limit, but rejected by MasterkeyFileValidator.DEFAULT
	public void testRejectsParametersExceedingLimit(int costParam, int blockSize) {
		var file = masterkeyFile(costParam, blockSize);

		Assertions.assertThrows(UnsupportedScryptParametersException.class, () -> CryptomatorMKFValidator.INSTANCE.validate(file));
	}

	private static MasterkeyFile masterkeyFile(int costParam, int blockSize) {
		var file = new MasterkeyFile();
		file.scryptCostParam = costParam;
		file.scryptBlockSize = blockSize;
		return file;
	}

}
