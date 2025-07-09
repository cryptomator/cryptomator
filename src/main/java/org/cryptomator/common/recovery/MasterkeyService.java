package org.cryptomator.common.recovery;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.cryptofs.common.Constants.DATA_DIR_NAME;

public final class MasterkeyService {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyService.class);

	private MasterkeyService() {}

	public static void recoverFromRecoveryKey(String recoveryKey, RecoveryKeyFactory recoveryKeyFactory, Path recoveryPath, CharSequence newPassword) throws IOException {
		recoveryKeyFactory.newMasterkeyFileWithPassphrase(recoveryPath, recoveryKey, newPassword);
	}

	public static Masterkey load(MasterkeyFileAccess masterkeyFileAccess, Path masterkeyFilePath, CharSequence password) throws IOException {
		return masterkeyFileAccess.load(masterkeyFilePath, password);
	}

	public static Optional<CryptorProvider.Scheme> validateRecoveryKeyAndDetectCombo(RecoveryKeyFactory recoveryKeyFactory, //
																					 Vault vault, String recoveryKey, //
																					 MasterkeyFileAccess masterkeyFileAccess) throws IllegalArgumentException {
		String tmpPass = UUID.randomUUID().toString();
		try (RecoveryDirectory recoveryDirectory = RecoveryDirectory.create(vault.getPath())) {
			Path tempRecoveryPath = recoveryDirectory.getRecoveryPath();
			recoverFromRecoveryKey(recoveryKey, recoveryKeyFactory, tempRecoveryPath, tmpPass);
			Path masterkeyFilePath = tempRecoveryPath.resolve(MASTERKEY_FILENAME);

			try (Masterkey mk = load(masterkeyFileAccess, masterkeyFilePath, tmpPass)) {
				return detect(mk, vault.getPath());
			} catch (IOException | CryptoException e) {
				LOG.info("Recovery key validation failed", e);
				return Optional.empty();
			}
		} catch (IOException | CryptoException e) {
			LOG.info("Recovery key validation failed");
		}
		return Optional.empty();
	}

	public static Optional<CryptorProvider.Scheme> detect(Masterkey masterkey, Path vaultPath) {
		try (Stream<Path> paths = Files.walk(vaultPath.resolve(DATA_DIR_NAME))) {
			List<String> excludedFilenames = List.of("dirid.c9r", "dir.c9r");
			Optional<Path> c9rFile = paths.filter(p -> p.toString().endsWith(".c9r")).filter(p -> excludedFilenames.stream().noneMatch(p.toString()::endsWith)).findFirst();
			if (c9rFile.isEmpty()) {
				LOG.info("Unable to detect Crypto scheme: No *.c9r file found in {}", vaultPath);
				return Optional.empty();
			}
			return determineScheme(c9rFile.get(), masterkey);
		} catch (IOException e) {
			LOG.info("Unable to detect Crypto scheme: Failed to inspect vault", e);
			return Optional.empty();
		}
	}


	private static Optional<CryptorProvider.Scheme> determineScheme(Path c9rFile, Masterkey masterkey) {
		return Arrays.stream(CryptorProvider.Scheme.values()).filter(scheme -> {
			try (Cryptor cryptor = CryptorProvider.forScheme(scheme).provide(masterkey.copy(), SecureRandom.getInstanceStrong())) {
				int headerSize = cryptor.fileHeaderCryptor().headerSize();

				ByteBuffer headerBuf = ByteBuffer.allocate(headerSize);

				try (FileChannel channel = FileChannel.open(c9rFile, StandardOpenOption.READ)) {
					channel.read(headerBuf, 0);
				}

				headerBuf.flip();
				cryptor.fileHeaderCryptor().decryptHeader(headerBuf.duplicate());
				LOG.debug("Detected Crypto scheme: {}", scheme);
				return true;
			} catch (CryptoException e) {
				LOG.debug("Could not decrypt with scheme: {}", scheme);
				return false;
			} catch (IOException | NoSuchAlgorithmException e) {
				LOG.warn("Unable to detect Crypto scheme: Failed to decrypt .c9r file", e);
				return false;
			}
		}).findFirst();
	}

}
