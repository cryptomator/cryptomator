package org.cryptomator.filesystem.crypto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.NoCryptor;
import org.cryptomator.filesystem.ReadableFile;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CryptoReadableFileTest {

	@Test(expected = UncheckedIOException.class)
	public void testPassthroughExceptions() {
		FileContentCryptor fileContentCryptor = new NoCryptor().getFileContentCryptor();

		// return a valid header but throw exception on consecutive read attempts:
		ReadableFile underlyingFile = Mockito.mock(ReadableFile.class);
		Mockito.when(underlyingFile.read(Mockito.any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
				buf.position(fileContentCryptor.getHeaderSize());
				return fileContentCryptor.getHeaderSize();
			}
		}).thenThrow(new UncheckedIOException(new IOException("failed.")));

		@SuppressWarnings("resource")
		ReadableFile cryptoReadableFile = new CryptoReadableFile(fileContentCryptor, underlyingFile);
		cryptoReadableFile.read(ByteBuffer.allocate(1));
	}

}
