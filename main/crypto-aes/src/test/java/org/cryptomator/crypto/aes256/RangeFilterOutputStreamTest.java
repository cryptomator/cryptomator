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

public class RangeFilterOutputStreamTest {

	@Test
	public void testNoOffsetUnlimited() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new RangeFilterOutputStream(out, 0l, Long.MAX_VALUE);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 0, 256);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	@Test
	public void testNoOffsetButLimit() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new RangeFilterOutputStream(out, 0l, 97l);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 0, 97);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	@Test
	public void testNoLimitButOffset() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new RangeFilterOutputStream(out, 43l, Long.MAX_VALUE);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 43, 256);
		Assert.assertArrayEquals(expected, out.toByteArray());
	}

	@Test
	public void testOffsettedAndLimited() throws IOException {
		final byte[] testData = createTestData(256);
		final InputStream in = new ByteArrayInputStream(testData);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final OutputStream decorator = new RangeFilterOutputStream(out, 43l, 57l);
		IOUtils.copy(in, decorator);

		final byte[] expected = Arrays.copyOfRange(testData, 43, 100);
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
