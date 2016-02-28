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
	private final Integer supportedVersion;

	public UnsupportedVaultFormatException(Integer detectedVersion, Integer supportedVersion) {
		super("Tried to open vault of version " + detectedVersion + ", but can only handle version " + supportedVersion);
		this.detectedVersion = detectedVersion;
		this.supportedVersion = supportedVersion;
	}

	public Integer getDetectedVersion() {
		return detectedVersion;
	}

	public Integer getSupportedVersion() {
		return supportedVersion;
	}

	public boolean isVaultOlderThanSoftware() {
		return detectedVersion == null || detectedVersion < supportedVersion;
	}

	public boolean isSoftwareOlderThanVault() {
		return detectedVersion > supportedVersion;
	}

}
