package org.cryptomator.crypto.exceptions;

public class EncryptFailedException extends CryptingException {
	private static final long serialVersionUID = -3855673600374897828L;

	public EncryptFailedException(Throwable t) {
		super("Encryption failed.", t);
	}

	public EncryptFailedException(String msg) {
		super(msg);
	}
}