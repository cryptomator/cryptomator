package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.cryptolib.api.Masterkey;

import java.security.interfaces.ECPrivateKey;

@FunctionalInterface
interface ReceivedKey {

	Masterkey decryptMasterkey(ECPrivateKey deviceKey);

	static ReceivedKey userAndDeviceKey(JWEObject userToken, JWEObject deviceToken) {
		return deviceKey -> {
			var userKey = JWEHelper.decryptUserKey(deviceToken, deviceKey);
			return JWEHelper.decryptVaultKey(userToken, userKey);
		};
	}

	static ReceivedKey legacyDeviceKey(JWEObject legacyAccessToken) {
		return deviceKey -> JWEHelper.decryptVaultKey(legacyAccessToken, deviceKey);
	}

}
