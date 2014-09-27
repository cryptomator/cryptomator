package de.sebastianstenzel.oce.crypto.exceptions;

public class DecryptFailedException extends StorageCryptingException {
	private static final long serialVersionUID = -3855673600374897828L;

	public DecryptFailedException(Throwable t) {
		super("Decryption failed.", t);
	}
}