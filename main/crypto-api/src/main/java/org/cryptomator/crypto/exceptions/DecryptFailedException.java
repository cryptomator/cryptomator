package org.cryptomator.crypto.exceptions;

public class DecryptFailedException extends CryptingException {
	private static final long serialVersionUID = -3855673600374897828L;

	public DecryptFailedException(Throwable t) {
		super("Decryption failed.", t);
	}

	public DecryptFailedException(String msg) {
		super(msg);
	}
}