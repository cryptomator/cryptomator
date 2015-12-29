/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

class ThreadLocalMac extends ThreadLocal<Mac>implements Supplier<Mac> {

	private final SecretKey macKey;
	private final String macAlgorithm;

	ThreadLocalMac(SecretKey macKey, String macAlgorithm) {
		this.macKey = macKey;
		this.macAlgorithm = macAlgorithm;
	}

	@Override
	protected Mac initialValue() {
		try {
			Mac mac = Mac.getInstance(macAlgorithm);
			mac.init(macKey);
			return mac;
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Could not create MAC.", e);
		}
	}

	@Override
	public Mac get() {
		Mac mac = super.get();
		mac.reset();
		return mac;
	}

}