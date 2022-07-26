package org.cryptomator.common.settings;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.io.BaseEncoding;
import org.cryptomator.common.Environment;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.cryptolib.common.Pkcs12Exception;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

@Singleton
public class DeviceKey {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceKey.class);
	private static final String KEYCHAIN_KEY = "cryptomator-device-p12";

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
		char[] passphrase = null;
		try {
			passphrase = keychainManager.loadPassphrase(KEYCHAIN_KEY);
			if (passphrase != null) {
				return loadExistingKeyPair(passphrase);
			} else {
				passphrase = randomPassword();
				keychainManager.storePassphrase(KEYCHAIN_KEY, CharBuffer.wrap(passphrase));
				return createAndStoreNewKeyPair(passphrase);
			}
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

	private P384KeyPair loadExistingKeyPair(char[] passphrase) throws IOException {
		var p12File = env.getP12Path() //
				.filter(Files::isRegularFile) //
				.findFirst() //
				.orElseThrow(() -> new DeviceKeyRetrievalException("Missing .p12 file"));
		LOG.debug("Loading existing device key from {}", p12File);
		return P384KeyPair.load(p12File, passphrase);
	}

	private P384KeyPair createAndStoreNewKeyPair(char[] passphrase) throws IOException {
		var p12File = env.getP12Path() //
				.findFirst() //
				.orElseThrow(() -> new DeviceKeyRetrievalException("No path for .p12 file configured"));
		var keyPair = P384KeyPair.generate();
		LOG.debug("Store new device key to {}", p12File);
		keyPair.store(p12File, passphrase);
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
