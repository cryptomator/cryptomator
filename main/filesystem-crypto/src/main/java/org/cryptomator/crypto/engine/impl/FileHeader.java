/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;

import org.cryptomator.crypto.engine.AuthenticationFailedException;

class FileHeader implements Destroyable {

	static final int HEADER_SIZE = 88;

	private static final int IV_POS = 0;
	private static final int IV_LEN = 16;
	private static final int PAYLOAD_POS = 16;
	private static final int PAYLOAD_LEN = 40;
	private static final int MAC_POS = 56;
	private static final int MAC_LEN = 32;

	private final byte[] iv;
	private final FileHeaderPayload payload;

	public FileHeader(SecureRandom randomSource) {
		this.iv = new byte[IV_LEN];
		this.payload = new FileHeaderPayload(randomSource);
		randomSource.nextBytes(iv);
	}

	private FileHeader(byte[] iv, FileHeaderPayload payload) {
		this.iv = iv;
		this.payload = payload;
	}

	public byte[] getIv() {
		return iv;
	}

	public FileHeaderPayload getPayload() {
		return payload;
	}

	public ByteBuffer toByteBuffer(SecretKey headerKey, Supplier<Mac> hmacSha256Factory) {
		ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE);
		result.position(IV_POS).limit(IV_POS + IV_LEN);
		result.put(iv);
		result.position(PAYLOAD_POS).limit(PAYLOAD_POS + PAYLOAD_LEN);
		result.put(payload.toCiphertextByteBuffer(headerKey, iv));
		ByteBuffer resultSoFar = result.asReadOnlyBuffer();
		resultSoFar.flip();
		Mac mac = hmacSha256Factory.get();
		assert mac.getMacLength() == MAC_LEN;
		mac.update(resultSoFar);
		result.position(MAC_POS).limit(MAC_POS + MAC_LEN);
		result.put(mac.doFinal());
		result.flip();
		return result;
	}

	@Override
	public boolean isDestroyed() {
		return payload.isDestroyed();
	}

	@Override
	public void destroy() {
		payload.destroy();
	}

	public static FileHeader decrypt(SecretKey headerKey, Supplier<Mac> hmacSha256Factory, ByteBuffer header) throws IllegalArgumentException, AuthenticationFailedException {
		if (header.remaining() != HEADER_SIZE) {
			throw new IllegalArgumentException("Invalid header size.");
		}

		checkHeaderMac(header, hmacSha256Factory.get());

		final byte[] iv = new byte[IV_LEN];
		final ByteBuffer ivBuf = header.asReadOnlyBuffer();
		ivBuf.position(IV_POS).limit(IV_POS + IV_LEN);
		ivBuf.get(iv);

		final ByteBuffer payloadBuf = header.asReadOnlyBuffer();
		payloadBuf.position(PAYLOAD_POS).limit(PAYLOAD_POS + PAYLOAD_LEN);

		final FileHeaderPayload payload = FileHeaderPayload.fromCiphertextByteBuffer(payloadBuf, headerKey, iv);

		return new FileHeader(iv, payload);
	}

	private static void checkHeaderMac(ByteBuffer header, Mac mac) throws AuthenticationFailedException {
		assert mac.getMacLength() == MAC_LEN;
		ByteBuffer headerData = header.asReadOnlyBuffer();
		headerData.position(0).limit(MAC_POS);
		mac.update(headerData);
		ByteBuffer headerMac = header.asReadOnlyBuffer();
		headerMac.position(MAC_POS).limit(MAC_POS + MAC_LEN);
		byte[] expectedMac = new byte[MAC_LEN];
		headerMac.get(expectedMac);

		if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
			throw new AuthenticationFailedException("Corrupt header.");
		}
	}

}
