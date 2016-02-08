/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;

class NoFilenameCryptor implements FilenameCryptor {

	private static final BaseNCodec BASE32 = new Base32();
	private static final ThreadLocal<MessageDigest> SHA1 = new ThreadLocalSha1();

	@Override
	public String hashDirectoryId(String cleartextDirectoryId) {
		final byte[] cleartextBytes = cleartextDirectoryId.getBytes(UTF_8);
		final byte[] hashedBytes = SHA1.get().digest(cleartextBytes);
		return BASE32.encodeAsString(hashedBytes);
	}

	@Override
	public boolean isEncryptedFilename(String ciphertextName) {
		return true;
	}

	@Override
	public String encryptFilename(String cleartextName, byte[]... associatedData) {
		return cleartextName;
	}

	@Override
	public String decryptFilename(String ciphertextName, byte[]... associatedData) {
		return ciphertextName;
	}

	private static class ThreadLocalSha1 extends ThreadLocal<MessageDigest> {

		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new AssertionError("SHA-1 exists in every JVM");
			}
		}

		@Override
		public MessageDigest get() {
			final MessageDigest sha1 = super.get();
			sha1.reset();
			return sha1;
		}
	}

}
