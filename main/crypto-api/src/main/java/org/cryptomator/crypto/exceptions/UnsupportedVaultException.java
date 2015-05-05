package org.cryptomator.crypto.exceptions;

public class UnsupportedVaultException extends Exception {

	private static final long serialVersionUID = -5147549533387945622L;

	private final Integer detectedVersion;
	private final Integer supportedVersion;

	public UnsupportedVaultException(Integer detectedVersion, Integer supportedVersion) {
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
