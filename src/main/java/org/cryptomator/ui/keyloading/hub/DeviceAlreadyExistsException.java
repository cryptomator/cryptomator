package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;

/**
 * Thrown, when Hub registerDevice-Request returns with 409
 */
class DeviceAlreadyExistsException extends MasterkeyLoadingFailedException {
	public DeviceAlreadyExistsException() {
		super("Device already registered on this Hub instance");
	}
}
