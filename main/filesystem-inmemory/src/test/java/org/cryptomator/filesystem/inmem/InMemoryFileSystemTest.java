/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryFileSystemTest {

	@Test
	public void testFolderCreation() throws IOException {
		final FileSystem fs = new InMemoryFileSystem();
		Folder fooFolder = fs.folder("foo");

		// nothing happened yet:
		Assert.assertFalse(fooFolder.exists());
		Assert.assertEquals(0, fs.folders().count());

		// create /foo
		fooFolder.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
		Assert.assertTrue(fooFolder.exists());
		Assert.assertEquals(1, fs.folders().count());

		// delete /foo
		fooFolder.delete();
		Assert.assertFalse(fooFolder.exists());
		Assert.assertEquals(0, fs.folders().count());

		// create /foo/bar
		Folder fooBarFolder = fooFolder.folder("bar");
		Assert.assertFalse(fooBarFolder.exists());
		fooBarFolder.create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(fooFolder.exists());
		Assert.assertTrue(fooBarFolder.exists());
		Assert.assertEquals(1, fs.folders().count());
		Assert.assertEquals(1, fooFolder.folders().count());
	}

	@Test
	public void testFileReadCopyMoveWrite() throws IOException, TimeoutException {
		final FileSystem fs = new InMemoryFileSystem();
		File fooFile = fs.file("foo.txt");

		// nothing happened yet:
		Assert.assertFalse(fooFile.exists());
		Assert.assertEquals(0, fs.files().count());

		// write "hello world" to foo
		try (WritableFile writable = fooFile.openWritable(1, TimeUnit.SECONDS)) {
			writable.write(ByteBuffer.wrap("hello".getBytes()));
			writable.write(ByteBuffer.wrap(" ".getBytes()));
			writable.write(ByteBuffer.wrap("world".getBytes()));
		}
		Assert.assertTrue(fooFile.exists());

		// copy foo to bar
		File barFile = fs.file("bar.txt");
		try (WritableFile writable = barFile.openWritable(1, TimeUnit.SECONDS)) {
			try (ReadableFile readable = fooFile.openReadable(1, TimeUnit.SECONDS)) {
				readable.copyTo(writable);
			}
		}
		Assert.assertTrue(fooFile.exists());
		Assert.assertTrue(barFile.exists());

		// move bar to baz
		File bazFile = fs.file("baz.txt");
		try (WritableFile src = barFile.openWritable(1, TimeUnit.SECONDS)) {
			try (WritableFile dst = bazFile.openWritable(1, TimeUnit.SECONDS)) {
				src.moveTo(dst);
			}
		}
		Assert.assertFalse(barFile.exists());
		Assert.assertTrue(bazFile.exists());

		// read "hello world" from baz
		final ByteBuffer readBuf = ByteBuffer.allocate(5);
		try (ReadableFile readable = bazFile.openReadable(1, TimeUnit.SECONDS)) {
			readable.read(readBuf, 6);
		}
		Assert.assertEquals("world", new String(readBuf.array()));

	}

}
