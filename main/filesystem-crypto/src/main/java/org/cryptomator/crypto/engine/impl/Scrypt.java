/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import org.bouncycastle.crypto.generators.SCrypt;

final class Scrypt {

	private Scrypt() {
	}

	/**
	 * Derives a key from the given passphrase.
	 * This implementation makes sure, any copies of the passphrase used during key derivation are overwritten in memory asap (before next GC cycle).
	 * 
	 * @param passphrase The passphrase
	 * @param salt Salt, ideally randomly generated
	 * @param costParam Cost parameter <code>N</code>, larger than 1, a power of 2 and less than <code>2^(128 * costParam / 8)</code>
	 * @param blockSize Block size <code>r</code>
	 * @param keyLengthInBytes Key output length <code>dkLen</code>
	 * @return Derived key
	 * @see <a href="https://tools.ietf.org/html/draft-josefsson-scrypt-kdf-04#section-2">RFC Draft</a>
	 */
	public static byte[] scrypt(CharSequence passphrase, byte[] salt, int costParam, int blockSize, int keyLengthInBytes) {
		// This is an attempt to get the password bytes without copies of the password being created in some dark places inside the JVM:
		final ByteBuffer buf = UTF_8.encode(CharBuffer.wrap(passphrase));
		final byte[] pw = new byte[buf.remaining()];
		buf.get(pw);
		try {
			return SCrypt.generate(pw, salt, costParam, blockSize, 1, keyLengthInBytes);
		} finally {
			Arrays.fill(pw, (byte) 0); // overwrite bytes
			buf.rewind(); // just resets markers
			buf.put(pw); // this is where we overwrite the actual bytes
		}
	}

}
