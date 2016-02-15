package org.cryptomator.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(Theories.class)
public class FileContentsTest {

	@DataPoints
	public static final Iterable<Charset> CHARSETS = Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.US_ASCII, StandardCharsets.UTF_16);

	@DataPoints
	public static final Iterable<String> TEST_CONTENTS = Arrays.asList("hello world", "hellö wörld", "");

	@Theory
	public void testReadAll(Charset charset, String testString) {
		Assume.assumeTrue(charset.newEncoder().canEncode(testString));

		ByteBuffer testContent = ByteBuffer.wrap(testString.getBytes(charset));
		File file = Mockito.mock(File.class);
		ReadableFile readable = Mockito.mock(ReadableFile.class);
		Mockito.when(file.openReadable()).thenReturn(readable);
		Mockito.when(readable.read(Mockito.any(ByteBuffer.class))).then(invocation -> {
			ByteBuffer target = invocation.getArgumentAt(0, ByteBuffer.class);
			if (testContent.hasRemaining()) {
				return ByteBuffers.copy(testContent, target);
			} else {
				return -1;
			}
		});

		String contentsRead = FileContents.withCharset(charset).readContents(file);
		Assert.assertEquals(testString, contentsRead);
	}

	@Theory
	public void testWriteAll(Charset charset, String testString) {
		Assume.assumeTrue(charset.newEncoder().canEncode(testString));

		ByteBuffer testContent = ByteBuffer.allocate(100);
		File file = Mockito.mock(File.class);
		WritableFile writable = Mockito.mock(WritableFile.class);
		Mockito.when(file.openWritable()).thenReturn(writable);
		Mockito.doAnswer(invocation -> {
			testContent.clear();
			return null;
		}).when(writable).truncate();
		Mockito.when(writable.write(Mockito.any(ByteBuffer.class))).then(invocation -> {
			ByteBuffer source = invocation.getArgumentAt(0, ByteBuffer.class);
			if (testContent.hasRemaining()) {
				return ByteBuffers.copy(source, testContent);
			} else {
				return -1;
			}
		});

		FileContents.withCharset(charset).writeContents(file, testString);
		Assert.assertArrayEquals(testString.getBytes(charset), Arrays.copyOf(testContent.array(), testContent.position()));
	}

	@Test(expected = UncheckedIOException.class)
	public void testIOExceptionDuringRead() {
		File file = Mockito.mock(File.class);
		Mockito.when(file.openReadable()).thenAnswer(invocation -> {
			throw new IOException("failed");
		});

		FileContents.UTF_8.readContents(file);
	}

	@Test(expected = UncheckedIOException.class)
	public void testUncheckedIOExceptionDuringRead() {
		File file = Mockito.mock(File.class);
		Mockito.when(file.openReadable()).thenThrow(new UncheckedIOException(new IOException("failed")));

		FileContents.UTF_8.readContents(file);
	}

	@Test(expected = UncheckedIOException.class)
	public void testUncheckedIOExceptionDuringWrite() {
		File file = Mockito.mock(File.class);
		Mockito.when(file.openWritable()).thenThrow(new UncheckedIOException(new IOException("failed")));

		FileContents.UTF_8.writeContents(file, "hello world");
	}

}
