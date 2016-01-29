/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.stats;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Test;
import org.mockito.Mockito;

public class StatsFileTest {

	@Test
	public void testStatsDuringRead() {
		ReadableFile readable = Mockito.mock(ReadableFile.class);
		File file = Mockito.mock(File.class);
		Mockito.when(file.openReadable()).thenReturn(readable);

		@SuppressWarnings("unchecked")
		Consumer<Long> readCounter = Mockito.mock(Consumer.class);
		File statsFile = new StatsFile(null, file, readCounter, null);

		Mockito.when(readable.read(Mockito.any())).thenReturn(123);
		try (ReadableFile r = statsFile.openReadable()) {
			r.read(ByteBuffer.allocate(0));
		}

		Mockito.verify(readCounter).accept(123l);
	}

	@Test
	public void testStatsDuringWrite() {
		WritableFile writable = Mockito.mock(WritableFile.class);
		File file = Mockito.mock(File.class);
		Mockito.when(file.openWritable()).thenReturn(writable);

		@SuppressWarnings("unchecked")
		Consumer<Long> writeCounter = Mockito.mock(Consumer.class);
		File statsFile = new StatsFile(null, file, null, writeCounter);

		Mockito.when(writable.write(Mockito.any())).thenReturn(123);
		try (WritableFile w = statsFile.openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}

		Mockito.verify(writeCounter).accept(123l);
	}

}
