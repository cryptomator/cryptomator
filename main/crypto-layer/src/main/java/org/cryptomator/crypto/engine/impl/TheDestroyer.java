package org.cryptomator.crypto.engine.impl;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

final class TheDestroyer {

	private TheDestroyer() {

	}

	public static void destroyQuietly(Destroyable d) {
		try {
			d.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
	}

}
