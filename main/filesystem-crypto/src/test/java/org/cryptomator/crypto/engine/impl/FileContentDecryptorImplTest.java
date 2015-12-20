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
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwN74OFIGKQKgsI7bakfCYm1VXJZiKFLyhZkQCz0Ye/il0PmdZOYsSYEH9h6S00RsdHL3wLtB1FJsb9QLTtP00H8M2theZaZdlKTmjhXsmbc=");
		final byte[] content = Base64.decode("tPCsFM1g/ubfJMa+AocdPh/WPHfXMFRJdIz6PkLuRijSIIXvxn7IUwVzHQ==");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header))) {
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 15)));
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 15, 43)));
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
