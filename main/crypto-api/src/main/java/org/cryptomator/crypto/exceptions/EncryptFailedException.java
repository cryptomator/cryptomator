package org.cryptomator.crypto.exceptions;

public class EncryptFailedException extends StorageCryptingException {
	private static final long serialVersionUID = -3855673600374897828L;

	public EncryptFailedException(String msg) {
		super(msg);
	}
}