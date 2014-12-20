package org.cryptomator.crypto.aes256;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class LimitFilterOutputStreamTest {

	@Test
	public void testNoLimit() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new LimitFilterOutputStream(out, Long.MAX_VALUE);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 0, 256);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	@Test
	public void testLimit43() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new LimitFilterOutputStream(out, 43l);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 0, 43);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	@Test
	public void testLimit307() throws IOException {
		final byte[] testData = createTestData(512);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new LimitFilterOutputStream(out, 307l);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 0, 307);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	private byte[] createTestData(int length) {
		final byte[] testData = new byte[length];
		for (int i = 0; i < length; i++) {
			testData[i] = (byte) i;
		}
		return testData;
	}

}
