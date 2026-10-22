package org.cryptomator.common.vaults;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class NotAVaultDirectoryException extends NoSuchFileException {

	public enum Reason {
		MISSING_DATA_DIR,
		DATA_NOT_A_DIRECTORY,
		MISSING_VAULT_CONFIG,
		VAULT_CONFIG_ACCESS_DENIED,
		UNSUPPORTED_STRUCTURE
	}

	private final transient Path path;
	private final Reason reason;

	public NotAVaultDirectoryException(Path path, Reason reason) {
		super(path.toString(), null, "Not a vault directory: " + reason);
		this.path = path;
		this.reason = reason;
	}

	public Path path() {
		return path;
	}

	public Reason notAVaultReason() {
		return reason;
	}
}
