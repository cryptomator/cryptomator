/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
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

		Runnable noop = () -> {
		};

		@SuppressWarnings("resource")
		ReadableFile cryptoReadableFile = new CryptoReadableFile(fileContentCryptor, underlyingFile, true, noop);
		cryptoReadableFile.read(ByteBuffer.allocate(1));
	}

}
