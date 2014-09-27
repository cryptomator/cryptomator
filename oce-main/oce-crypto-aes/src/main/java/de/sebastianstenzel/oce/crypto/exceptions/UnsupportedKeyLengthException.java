package de.sebastianstenzel.oce.crypto.exceptions;

public class UnsupportedKeyLengthException extends StorageCryptingException {
	private static final long serialVersionUID = 8114147446419390179L;
	
	public UnsupportedKeyLengthException(int length, int maxLength) {
		super(String.format("Key length (%i) exceeds policy maximum (%i).", length, maxLength));
	}
	
}