package org.cryptomator.common.recovery;

import java.io.IOException;
import java.nio.file.Path;

import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.MasterkeyLoader;

import static org.cryptomator.common.Constants.DEFAULT_KEY_ID;

public final class CryptoFsInitializer {

	private CryptoFsInitializer() {}

	public static void init(Path recoveryPath,
							Masterkey masterkey,
							int shorteningThreshold,
							CryptorProvider.Scheme scheme) throws IOException, CryptoException {

		MasterkeyLoader loader = ignored -> masterkey.copy();
		CryptoFileSystemProperties fsProps = CryptoFileSystemProperties //
				.cryptoFileSystemProperties() //
				.withCipherCombo(scheme) //
				.withKeyLoader(loader) //
				.withShorteningThreshold(shorteningThreshold) //
				.build();
		CryptoFileSystemProvider.initialize(recoveryPath, fsProps, DEFAULT_KEY_ID);
	}
}
