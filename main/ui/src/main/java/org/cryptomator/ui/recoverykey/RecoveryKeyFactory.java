package org.cryptomator.ui.recoverykey;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import org.cryptomator.cryptofs.common.MasterkeyBackupHelper;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFile;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.PEPPER;

@Singleton
public class RecoveryKeyFactory {

	private static final String MASTERKEY_BACKUP_SUFFIX = ".bkup";

	private final WordEncoder wordEncoder;
	private final SecureRandom csprng;

	@Inject
	public RecoveryKeyFactory(WordEncoder wordEncoder, SecureRandom csprng) {
		this.wordEncoder = wordEncoder;
		this.csprng = csprng;
	}

	public Collection<String> getDictionary() {
		return wordEncoder.getWords();
	}

	/**
	 * @param vaultPath Path to the storage location of a vault
	 * @param password The vault's password
	 * @return The recovery key of the vault at the given path
	 * @throws IOException If the masterkey file could not be read
	 * @throws InvalidPassphraseException If the provided password is wrong
	 * @throws CryptoException In case of other cryptographic errors
	 * @apiNote This is a long-running operation and should be invoked in a background thread
	 */
	public String createRecoveryKey(Path vaultPath, CharSequence password) throws IOException, InvalidPassphraseException, CryptoException {
		Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
		byte[] rawKey = new byte[0];
		try (var masterkey = MasterkeyFile.withContentFromFile(masterkeyPath).unlock(password, PEPPER, Optional.empty()).loadKeyAndClose()) {
			rawKey = masterkey.getEncoded();
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
	 * Creates a completely new masterkey using a recovery key.
	 *
	 * @param vaultPath Path to the storage location of a vault
	 * @param recoveryKey A recovery key for this vault
	 * @param newPassword The new password used to encrypt the keys
	 * @throws IOException If the masterkey file could not be written
	 * @throws IllegalArgumentException If the recoveryKey is invalid
	 * @apiNote This is a long-running operation and should be invoked in a background thread
	 */
	public void resetPasswordWithRecoveryKey(Path vaultPath, String recoveryKey, CharSequence newPassword) throws IOException, IllegalArgumentException {
		final byte[] rawKey = decodeRecoveryKey(recoveryKey);
		try (var masterkey = Masterkey.createFromRaw(rawKey)) {
			byte[] restoredKey = MasterkeyFile.lock(masterkey, newPassword, PEPPER, 999, csprng);
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			if (Files.exists(masterkeyPath)) {
				byte[] oldMasterkeyBytes = Files.readAllBytes(masterkeyPath);
				Path backupKeyPath = vaultPath.resolve(MASTERKEY_FILENAME + MasterkeyBackupHelper.generateFileIdSuffix(oldMasterkeyBytes) + MASTERKEY_BACKUP_SUFFIX);
				Files.move(masterkeyPath, backupKeyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			Files.write(masterkeyPath, restoredKey, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		} finally {
			Arrays.fill(rawKey, (byte) 0x00);
		}
	}

	/**
	 * Checks whether a String is a syntactically correct recovery key with a valid checksum
	 *
	 * @param recoveryKey A word sequence which might be a recovery key
	 * @return <code>true</code> if this seems to be a legitimate recovery key
	 */
	public boolean validateRecoveryKey(String recoveryKey) {
		try {
			byte[] key = decodeRecoveryKey(recoveryKey);
			Arrays.fill(key, (byte) 0x00);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private byte[] decodeRecoveryKey(String recoveryKey) throws IllegalArgumentException {
		byte[] paddedKey = new byte[0];
		try {
			paddedKey = wordEncoder.decode(recoveryKey);
			Preconditions.checkArgument(paddedKey.length == 66, "Recovery key doesn't consist of 66 bytes.");
			byte[] rawKey = Arrays.copyOf(paddedKey, 64);
			byte[] expectedCrc16 = Arrays.copyOfRange(paddedKey, 64, 66);
			byte[] actualCrc32 = Hashing.crc32().hashBytes(rawKey).asBytes();
			byte[] actualCrc16 = Arrays.copyOf(actualCrc32, 2);
			Preconditions.checkArgument(Arrays.equals(expectedCrc16, actualCrc16), "Recovery key has invalid CRC.");
			return rawKey;
		} finally {
			Arrays.fill(paddedKey, (byte) 0x00);
		}
	}

}
