package org.cryptomator.common;

import org.cryptomator.cryptolib.common.MasterkeyFile;
import org.cryptomator.cryptolib.common.MasterkeyFileValidator;

import java.io.IOException;

/**
 * Rejects masterkey files whose scrypt key derivation would claim too large a share of the heap, in addition to the checks of {@link MasterkeyFileValidator#DEFAULT}.
 */
public final class CryptomatorMKFValidator implements MasterkeyFileValidator {

	public static final CryptomatorMKFValidator INSTANCE = new CryptomatorMKFValidator();

	// Cryptomator writes masterkey files with N=2^15, r=8 (32 MiB). The launchers cap the heap at 256 MiB, so cryptolib's default limit of 1 GiB is far too generous.
	static final long MAX_SCRYPT_MEMORY = 64L * 1024 * 1024;

	private CryptomatorMKFValidator() {}

	@Override
	public void validate(MasterkeyFile file) throws UnsupportedScryptParametersException {
		try {
			MasterkeyFileValidator.DEFAULT.validate(file); // also bounds N and r, so the multiplication below cannot overflow
		} catch (IOException e) {
			throw new UnsupportedScryptParametersException(e.getMessage(), e);
		}
		if (128L * file.scryptBlockSize * file.scryptCostParam > MAX_SCRYPT_MEMORY) {
			throw new UnsupportedScryptParametersException("scrypt parameters exceed memory limit of " + MAX_SCRYPT_MEMORY + " bytes");
		}
	}

}
