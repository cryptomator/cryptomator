package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.io.ByteBuffers;
import org.junit.Assert;
import org.junit.Test;

public class FileContentDecryptorImplTest {

	@Test
	public void testDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAbQMxxKDDeVNbWcxRPUp3zSKaIl9RDlCco7Aa975ufw/3rL27hDTQEnd3FZNlWh1VHmi5hGO9Cn5n4hrsZARZQ8mJeLxjNKI4DZL72lGQKN4=");
		final byte[] content = Base64.decode("tPCsFM1g/ubfJMY0O2wdWwEHrRZG0HQPfeaAJxtXs7Xkq3g0idoVCp2BbUc=");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header))) {
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 10)));
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 10, 44)));
			decryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(11); // we just care about the first 11 bytes, as this is the ciphertext.
			ByteBuffer buf;
			while ((buf = decryptor.cleartext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}

			Assert.assertArrayEquals("hello world".getBytes(), result.array());
		}
	}

}
