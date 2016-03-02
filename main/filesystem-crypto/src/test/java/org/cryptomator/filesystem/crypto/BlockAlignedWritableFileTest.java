/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.nio.ByteBuffer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.crypto.BlockAlignedWritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BlockAlignedWritableFileTest {

	@Test
	public void testSwitchingModes() {
		FileSystem fs = new InMemoryFileSystem();
		File file = fs.file("test");
		try (WritableFile w = file.openWritable()) {
			w.write(ByteBuffer.wrap(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}));
		}

		BlockAlignedWritableFile writable = Mockito.spy(new BlockAlignedWritableFile(file::openWritable, file::openReadable, 2));
		writable.write(ByteBuffer.wrap(new byte[] {0x11, 0x12, 0x13}));
		Mockito.verify(writable, Mockito.never()).switchToBlockAlignedMode();
		writable.position(1);
		Mockito.verify(writable).switchToBlockAlignedMode();
		writable.write(ByteBuffer.wrap(new byte[] {0x14, 0x15, 0x16}));
		writable.close();

		try (ReadableFile r = file.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(10);
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x11, 0x14, 0x15, 0x16, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}, buf.array());
		}
	}

	@Test
	public void testWrite() {
		FileSystem fs = new InMemoryFileSystem();
		File file = fs.file("test");
		try (WritableFile w = file.openWritable()) {
			w.write(ByteBuffer.wrap(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}));
		}

		for (int i = 1; i < 12; i++) {
			testWrite(file, i);
		}
	}

	private void testWrite(File file, int blockSize) {
		try (WritableFile w = new BlockAlignedWritableFile(file::openWritable, file::openReadable, blockSize)) {
			w.position(4);
			w.write(ByteBuffer.wrap(new byte[] {0x11, 0x22, 0x33}));
		}

		try (ReadableFile r = file.openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(10);
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x03, 0x11, 0x22, 0x33, 0x07, 0x08, 0x09}, buf.array());
		}
	}

}
