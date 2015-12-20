package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;

class FileHeader implements Destroyable {

	static final int HEADER_SIZE = 104;

	private static final int IV_POS = 0;
	private static final int IV_LEN = 16;
	private static final int NONCE_POS = 16;
	private static final int NONCE_LEN = 8;
	private static final int PAYLOAD_POS = 24;
	private static final int PAYLOAD_LEN = 48;
	private static final int MAC_POS = 72;
	private static final int MAC_LEN = 32;

	private final byte[] iv;
	private final byte[] nonce;
	private final FileHeaderPayload payload;

	public FileHeader(SecureRandom randomSource) {
		this.iv = new byte[IV_LEN];
		this.nonce = new byte[NONCE_LEN];
		this.payload = new FileHeaderPayload(randomSource);
		randomSource.nextBytes(iv);
		randomSource.nextBytes(nonce);
	}

	private FileHeader(byte[] iv, byte[] nonce, FileHeaderPayload payload) {
		this.iv = iv;
		this.nonce = nonce;
		this.payload = payload;
	}

	public byte[] getIv() {
		return iv;
	}

	public byte[] getNonce() {
		return nonce;
	}

	public FileHeaderPayload getPayload() {
		return payload;
	}

	public ByteBuffer toByteBuffer(SecretKey headerKey, Supplier<Mac> hmacSha256Factory) {
		ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE);
		result.position(IV_POS).limit(IV_POS + IV_LEN);
		result.put(iv);
		result.position(NONCE_POS).limit(NONCE_POS + NONCE_LEN);
		result.put(nonce);
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

	public static FileHeader decrypt(SecretKey headerKey, Supplier<Mac> hmacSha256Factory, ByteBuffer header) throws IllegalArgumentException {
		if (header.remaining() != HEADER_SIZE) {
			throw new IllegalArgumentException("Invalid header size.");
		}

		checkHeaderMac(header, hmacSha256Factory.get());

		final byte[] iv = new byte[IV_LEN];
		final ByteBuffer ivBuf = header.asReadOnlyBuffer();
		ivBuf.position(IV_POS).limit(IV_POS + IV_LEN);
		ivBuf.get(iv);

		final byte[] nonce = new byte[NONCE_LEN];
		final ByteBuffer nonceBuf = header.asReadOnlyBuffer();
		nonceBuf.position(NONCE_POS).limit(NONCE_POS + NONCE_LEN);
		nonceBuf.get(nonce);

		final ByteBuffer payloadBuf = header.asReadOnlyBuffer();
		payloadBuf.position(PAYLOAD_POS).limit(PAYLOAD_POS + PAYLOAD_LEN);

		final FileHeaderPayload payload = FileHeaderPayload.fromCiphertextByteBuffer(payloadBuf, headerKey, iv);

		return new FileHeader(iv, nonce, payload);
	}

	private static void checkHeaderMac(ByteBuffer header, Mac mac) throws IllegalArgumentException {
		assert mac.getMacLength() == MAC_LEN;
		ByteBuffer headerData = header.asReadOnlyBuffer();
		headerData.position(0).limit(MAC_POS);
		mac.update(headerData);
		ByteBuffer headerMac = header.asReadOnlyBuffer();
		headerMac.position(MAC_POS).limit(MAC_POS + MAC_LEN);
		byte[] expectedMac = new byte[MAC_LEN];
		headerMac.get(expectedMac);

		if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
			throw new IllegalArgumentException("Corrupt header.");
		}
	}

}
