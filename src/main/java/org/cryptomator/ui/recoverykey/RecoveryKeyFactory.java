package org.cryptomator.ui.recoverykey;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import org.cryptomator.cryptofs.common.BackupHelper;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static org.cryptomator.common.Constants.MASTERKEY_BACKUP_SUFFIX;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@Singleton
public class RecoveryKeyFactory {

	private final WordEncoder wordEncoder;
	private final MasterkeyFileAccess masterkeyFileAccess;

	@Inject
	public RecoveryKeyFactory(WordEncoder wordEncoder, MasterkeyFileAccess masterkeyFileAccess) {
		this.wordEncoder = wordEncoder;
		this.masterkeyFileAccess = masterkeyFileAccess;
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
		try (var masterkey = masterkeyFileAccess.load(masterkeyPath, password)) {
			rawKey = masterkey.getEncoded();
			return createRecoveryKey(rawKey);
		} finally {
			Arrays.fill(rawKey, (byte) 0x00);
		}
	}

	@VisibleForTesting
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
	public void newMasterkeyFileWithPassphrase(Path vaultPath, String recoveryKey, CharSequence newPassword) throws IOException, IllegalArgumentException {
		final byte[] rawKey = decodeRecoveryKey(recoveryKey);
		try (var masterkey = new Masterkey(rawKey)) {
			Path masterkeyPath = vaultPath.resolve(MASTERKEY_FILENAME);
			if (Files.exists(masterkeyPath)) {
				byte[] oldMasterkeyBytes = Files.readAllBytes(masterkeyPath);
				// TODO: deduplicate with ChangePasswordController:
				Path backupKeyPath = vaultPath.resolve(MASTERKEY_FILENAME + BackupHelper.generateFileIdSuffix(oldMasterkeyBytes) + MASTERKEY_BACKUP_SUFFIX);
				Files.move(masterkeyPath, backupKeyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			masterkeyFileAccess.persist(masterkey, masterkeyPath, newPassword);
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
		return validateRecoveryKey(recoveryKey, null);
	}

	/**
	 * Checks whether a String is a syntactically correct recovery key with a valid checksum and passes the extended validation.
	 *
	 * @param recoveryKey A word sequence which might be a recovery key
	 * @param extendedValidation Additional verification of the decoded key (optional)
	 * @return <code>true</code> if this seems to be a legitimate recovery key and passes the extended validation
	 */
	public boolean validateRecoveryKey(String recoveryKey, @Nullable Predicate<byte[]> extendedValidation) {
		byte[] key = new byte[0];
		try {
			key = decodeRecoveryKey(recoveryKey);
			if (extendedValidation != null) {
				return extendedValidation.test(key);
			} else {
				return true;
			}
		} catch (IllegalArgumentException e) {
			return false;
		} finally {
			Arrays.fill(key, (byte) 0x00);
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
