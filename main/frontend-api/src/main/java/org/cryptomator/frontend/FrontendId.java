package org.cryptomator.frontend;

import static java.util.UUID.randomUUID;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class FrontendId implements Serializable {

	public static final String FRONTEND_ID_PATTERN = "[a-zA-Z0-9_-]{12}";

	public static FrontendId generate() {
		return new FrontendId();
	}

	public static FrontendId from(String value) {
		return new FrontendId(value);
	}

	private final String value;

	private FrontendId() {
		this(generateId());
	}

	private FrontendId(String value) {
		if (!value.matches(FRONTEND_ID_PATTERN)) {
			throw new IllegalArgumentException("Invalid frontend id " + value);
		}
		this.value = value;
	}

	private static String generateId() {
		return asBase64String(nineBytesFrom(randomUUID()));
	}

	private static String asBase64String(ByteBuffer bytes) {
		ByteBuffer base64Buffer = Base64.getUrlEncoder().encode(bytes);
		return new String(asByteArray(base64Buffer), StandardCharsets.US_ASCII);
	}

	private static ByteBuffer nineBytesFrom(UUID uuid) {
		ByteBuffer uuidBuffer = ByteBuffer.allocate(9);
		uuidBuffer.putLong(uuid.getMostSignificantBits());
		uuidBuffer.put((byte) (uuid.getLeastSignificantBits() & 0xFF));
		uuidBuffer.flip();
		return uuidBuffer;
	}

	private static byte[] asByteArray(ByteBuffer buffer) {
		if (buffer.hasArray()) {
			return buffer.array();
		} else {
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			return bytes;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return obj == this || internalEquals((FrontendId) obj);
	}

	private boolean internalEquals(FrontendId obj) {
		return value.equals(obj.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value;
	}

}
