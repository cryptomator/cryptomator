package org.cryptomator.filesystem.stats;

import java.nio.ByteBuffer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class StatsFileSystemTest {

	@Test
	public void testReadAndWriteCounters() {
		FileSystem underlyingFs = new InMemoryFileSystem();
		StatsFileSystem statsFs = new StatsFileSystem(underlyingFs);
		statsFs.folder("foo").create();
		File testFile = statsFs.folder("foo").file("bar");

		Assert.assertEquals(0l, statsFs.getBytesRead());
		Assert.assertEquals(0l, statsFs.getBytesWritten());

		try (WritableFile w = testFile.openWritable()) {
			w.write(ByteBuffer.allocate(15));
		}

		Assert.assertEquals(0l, statsFs.getBytesRead());
		Assert.assertEquals(15l, statsFs.getBytesWritten());

		statsFs.resetBytesWritten();

		Assert.assertEquals(0l, statsFs.getBytesRead());
		Assert.assertEquals(0l, statsFs.getBytesWritten());

		try (ReadableFile r = testFile.openReadable()) {
			r.read(ByteBuffer.allocate(3));
		}

		Assert.assertEquals(3l, statsFs.getBytesRead());
		Assert.assertEquals(0l, statsFs.getBytesWritten());

		statsFs.resetBytesRead();

		Assert.assertEquals(0l, statsFs.getBytesRead());
		Assert.assertEquals(0l, statsFs.getBytesWritten());
	}

}
