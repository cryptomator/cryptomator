package org.cryptomator.ui.recoverykey;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Singleton
public class RecoveryKeyFactory {

	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes
	
	private final WordEncoder wordEncoder;
	
	@Inject
	public RecoveryKeyFactory(WordEncoder wordEncoder) {
		this.wordEncoder = wordEncoder;
	}

	/**
	 * @param vaultPath Path to the storage location of a vault
	 * @param password The vault's password
	 * @return The recovery key of the vault at the given path
	 * @throws IOException If the masterkey file could not be read
	 * @throws InvalidPassphraseException If the provided password is wrong
	 * @apiNote This is a long-running operation and should be invoked in a background thread
	 */
	public String createRecoveryKey(Path vaultPath, CharSequence password) throws IOException, InvalidPassphraseException {
		byte[] rawKey = CryptoFileSystemProvider.exportRawKey(vaultPath, MASTERKEY_FILENAME, new byte[0], password);
		try {
			return createRecoveryKey(rawKey);
		} finally {
			Arrays.fill(rawKey, (byte) 0x00);
		}
	}

	// visible for testing
	String createRecoveryKey(byte[] rawKey) {
		Preconditions.checkArgument(rawKey.length == 64, "key should be 64 bytes");
		byte[] paddedKey = Arrays.copyOf(rawKey, 66);
		try {
			// copy 16 most significant bits of CRC32(rawKey) to the end of paddedKey:
			Hashing.crc32().hashBytes(rawKey).writeBytesTo(paddedKey, 64, 2);
			return wordEncoder.encodePadded(paddedKey);
		} finally {
			Arrays.fill(paddedKey, (byte) 0x00);
		}
	}

	/**
	 * Checks whether a String is a syntactically correct recovery key with a valid checksum
	 * @param recoveryKey A word sequence which might be a recovery key
	 * @return <code>true</code> if this seems to be a legitimate recovery key
	 */
	public boolean validateRecoveryKey(String recoveryKey) {
		final byte[] paddedKey;
		try {
			paddedKey = wordEncoder.decode(recoveryKey);
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (paddedKey.length != 66) {
			return false;
		}
		byte[] rawKey = Arrays.copyOf(paddedKey, 64);
		byte[] expectedCrc16 = Arrays.copyOfRange(paddedKey, 64, 66);
		byte[] actualCrc32 = Hashing.crc32().hashBytes(rawKey).asBytes();
		byte[] actualCrc16 = Arrays.copyOf(actualCrc32, 2);
		return Arrays.equals(expectedCrc16, actualCrc16);
	}

}
