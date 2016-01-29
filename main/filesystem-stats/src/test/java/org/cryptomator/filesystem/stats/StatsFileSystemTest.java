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

		Assert.assertEquals(0l, statsFs.getThenResetBytesRead());
		Assert.assertEquals(0l, statsFs.getThenResetBytesWritten());

		try (WritableFile w = testFile.openWritable()) {
			w.write(ByteBuffer.allocate(15));
		}

		Assert.assertEquals(0l, statsFs.getThenResetBytesRead());
		Assert.assertEquals(15l, statsFs.getThenResetBytesWritten());
		Assert.assertEquals(0l, statsFs.getThenResetBytesWritten());

		try (ReadableFile r = testFile.openReadable()) {
			r.read(ByteBuffer.allocate(3));
		}

		Assert.assertEquals(3l, statsFs.getThenResetBytesRead());
		Assert.assertEquals(0l, statsFs.getThenResetBytesRead());
		Assert.assertEquals(0l, statsFs.getThenResetBytesWritten());
	}

}
