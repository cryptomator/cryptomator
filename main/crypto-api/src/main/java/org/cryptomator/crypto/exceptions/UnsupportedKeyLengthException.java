package org.cryptomator.crypto.exceptions;

public class UnsupportedKeyLengthException extends StorageCryptingException {
	private static final long serialVersionUID = 8114147446419390179L;

	private final int requestedLength;
	private final int supportedLength;

	public UnsupportedKeyLengthException(int length, int maxLength) {
		super(String.format("Key length (%d) exceeds policy maximum (%d).", length, maxLength));
		this.requestedLength = length;
		this.supportedLength = maxLength;
	}

	public int getRequestedLength() {
		return requestedLength;
	}

	public int getSupportedLength() {
		return supportedLength;
	}

}