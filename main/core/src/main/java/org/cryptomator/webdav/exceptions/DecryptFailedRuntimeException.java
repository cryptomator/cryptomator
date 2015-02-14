package org.cryptomator.webdav.exceptions;

import org.cryptomator.crypto.exceptions.DecryptFailedException;

public class DecryptFailedRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -2726689824823439865L;

	public DecryptFailedRuntimeException(DecryptFailedException cause) {
		super(cause);
	}

	@Override
	public String getMessage() {
		return getCause().getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return getCause().getLocalizedMessage();
	}

}
