package org.cryptomator.common.settings;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import org.cryptomator.common.Environment;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.cryptolib.common.Pkcs12Exception;
import org.cryptomator.cryptolib.common.Pkcs12PasswordException;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

@Singleton
public class DeviceKey {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceKey.class);
	public static final String KEYCHAIN_KEY = "cryptomator-device-p12";
	public static final String KEYCHAIN_DISPLAY_NAME = "Cryptomator Device Keypair .p12 Passphrase";

	private final KeychainManager keychainManager;
	private final Environment env;
	private final SecureRandom csprng;
	private final Supplier<P384KeyPair> keyPairSupplier;

	@Inject
	public DeviceKey(KeychainManager keychainManager, Environment env, SecureRandom csprng) {
		this.keychainManager = keychainManager;
		this.env = env;
		this.csprng = csprng;
		this.keyPairSupplier = Suppliers.memoize(this::loadOrCreate);
	}

	public P384KeyPair get() throws DeviceKeyRetrievalException {
		Preconditions.checkState(keychainManager.isSupported());
		return keyPairSupplier.get();
	}

	private P384KeyPair loadOrCreate() throws DeviceKeyRetrievalException {
		Path p12File = env.getP12Path().findFirst().orElseThrow(() -> new DeviceKeyRetrievalException("No path for .p12 file configured"));
		char[] passphrase = null;
		try {
			passphrase = keychainManager.loadPassphrase(KEYCHAIN_KEY);
			if (passphrase != null && Files.isRegularFile(p12File)) {
				try {
					return loadExistingKeyPair(passphrase, p12File);
				} catch (Pkcs12PasswordException e) {
					LOG.warn("Stored passphrase does not match device key {}. Generating a new device key.", p12File);
				} finally {
					Arrays.fill(passphrase, '\0');
				}
			}
			// (re)generate the key pair if the file or passphrase is missing, or if the passphrase does not match the file
			passphrase = randomPassword();
			return createAndStoreNewKeyPair(passphrase, p12File);
		} catch (KeychainAccessException e) {
			throw new DeviceKeyRetrievalException("Failed to access system keychain", e);
		} catch (Pkcs12Exception | IOException e) {
			throw new DeviceKeyRetrievalException("Failed to access .p12 file", e);
		} finally {
			if (passphrase != null) {
				Arrays.fill(passphrase, '\0');
			}
		}
	}

	private P384KeyPair loadExistingKeyPair(char[] passphrase, Path p12File) throws IOException {
		LOG.debug("Loading existing device key from {}", p12File);
		return P384KeyPair.load(p12File, passphrase);
	}

	private P384KeyPair createAndStoreNewKeyPair(char[] passphrase, Path p12File) throws IOException, KeychainAccessException {
		var keyPair = P384KeyPair.generate();
		var tmpFile = p12File.resolveSibling(p12File.getFileName().toString() + ".tmp");
		if(Files.exists(tmpFile)) {
			LOG.debug("Leftover from devicekey creation detected. Deleting {}", tmpFile);
			Files.delete(tmpFile);
		}
		try (var out = Files.newOutputStream(tmpFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			keyPair.store(out, passphrase);
		}
		try {
			keychainManager.storePassphrase(KEYCHAIN_KEY, KEYCHAIN_DISPLAY_NAME, CharBuffer.wrap(passphrase));
		} catch (KeychainAccessException e) {
			try {
				Files.delete(tmpFile);
			} catch (IOException deleteException) {
				e.addSuppressed(deleteException);
			}
			throw e;
		}
		LOG.debug("Store new device key to {}", p12File);
		Files.move(tmpFile, p12File, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		return keyPair;
	}

	private char[] randomPassword() {
		// this is a fast & easy attempt to create a random string:
		var uuid = new UUID(csprng.nextLong(), csprng.nextLong());
		return uuid.toString().toCharArray();
	}

	public static class DeviceKeyRetrievalException extends RuntimeException {
		private DeviceKeyRetrievalException(String message) {
			super(message);
		}
		private DeviceKeyRetrievalException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
