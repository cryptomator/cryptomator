package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.cryptolib.api.Masterkey;

import java.security.interfaces.ECPrivateKey;

@FunctionalInterface
interface ReceivedKey {

	/**
	 * Decrypts the vault key.
	 *
	 * @param deviceKey This device's private key.
	 * @return The decrypted vault key
	 */
	Masterkey decryptMasterkey(ECPrivateKey deviceKey);

	/**
	 * Creates an unlock response object from the user key + vault key.
	 *
	 * @param vaultKeyJwe a JWE containing the symmetric vault key, encrypted for this device's user.
	 * @param userKeyJwe a JWE containing the user's private key, encrypted for this device.
	 * @return Ciphertext received by Hub, which can be decrypted using this device's private key.
	 */
	static ReceivedKey vaultKeyAndUserKey(JWEObject vaultKeyJwe, JWEObject userKeyJwe) {
		return deviceKey -> {
			var userKey = JWEHelper.decryptUserKey(userKeyJwe, deviceKey);
			return JWEHelper.decryptVaultKey(vaultKeyJwe, userKey);
		};
	}

	/**
	 * Creates an unlock response object from the received legacy "access token" JWE.
	 *
	 * @param vaultKeyJwe a JWE containing the symmetric vault key, encrypted for this device.
	 * @return Ciphertext received by Hub, which can be decrypted using this device's private key.
	 * @deprecated Only for compatibility with Hub 1.0 - 1.2
	 */
	@Deprecated
	static ReceivedKey legacyDeviceKey(JWEObject vaultKeyJwe) {
		return deviceKey -> JWEHelper.decryptVaultKey(vaultKeyJwe, deviceKey);
	}

}
