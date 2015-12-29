package org.cryptomator.filesystem.blockaligned;

import java.nio.ByteBuffer;

import org.bouncycastle.util.Arrays;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class BlockAlignedReadableFileTest {

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidBlockSize() {
		@SuppressWarnings(value = {"resource", "unused"})
		ReadableFile r = new BlockAlignedReadableFile(null, 0);
	}

	@Test
	public void testRead() {
		FileSystem fs = new InMemoryFileSystem();
		File file = fs.file("test");
		try (WritableFile w = file.openWritable()) {
			w.write(ByteBuffer.wrap(new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09}));
		}

		for (int i = 1; i < 12; i++) {
			testRead(file, i);
		}
	}

	private void testRead(File file, int blockSize) {
		try (ReadableFile r = new BlockAlignedReadableFile(file.openReadable(), blockSize)) {
			ByteBuffer buf = ByteBuffer.allocate(3);

			// 3...
			r.position(3);
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x03, 0x04, 0x05}, Arrays.copyOf(buf.array(), buf.remaining()));

			// go on...
			buf.clear();
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x06, 0x07, 0x08}, Arrays.copyOf(buf.array(), buf.remaining()));

			// go on till EOF...
			buf.clear();
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x09}, Arrays.copyOf(buf.array(), buf.remaining()));

			// back to 4...
			r.position(4);
			buf.clear();
			r.read(buf);
			buf.flip();
			Assert.assertArrayEquals(new byte[] {0x04, 0x05, 0x06}, Arrays.copyOf(buf.array(), buf.remaining()));
		}
	}

}
