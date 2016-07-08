/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

public class UnsupportedVaultFormatException extends CryptoException {

	private final Integer detectedVersion;
	private final Integer latestSupportedVersion;

	public UnsupportedVaultFormatException(Integer detectedVersion, Integer latestSupportedVersion) {
		super("Tried to open vault of version " + detectedVersion + ", latest supported version is " + latestSupportedVersion);
		this.detectedVersion = detectedVersion;
		this.latestSupportedVersion = latestSupportedVersion;
	}

	public Integer getDetectedVersion() {
		return detectedVersion;
	}

	public Integer getLatestSupportedVersion() {
		return latestSupportedVersion;
	}

	public boolean isVaultOlderThanSoftware() {
		return detectedVersion == null || detectedVersion < latestSupportedVersion;
	}

	public boolean isSoftwareOlderThanVault() {
		return detectedVersion > latestSupportedVersion;
	}

}
