/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

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
	public void testFolderCreation() {
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
	public void testFileReadCopyMoveWrite() throws TimeoutException {
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

	@Test
	public void testFolderCopy() throws TimeoutException {
		final FileSystem fs = new InMemoryFileSystem();
		final Folder fooBarFolder = fs.folder("foo").folder("bar");
		final Folder qweAsdFolder = fs.folder("qwe").folder("asd");
		final Folder qweAsdBarFolder = qweAsdFolder.folder("bar");
		final File test1File = fooBarFolder.file("test1.txt");
		final File test2File = fooBarFolder.file("test2.txt");
		fooBarFolder.create(FolderCreateMode.INCLUDING_PARENTS);

		// create some files inside foo/bar/
		try (WritableFile writable1 = test1File.openWritable(1, TimeUnit.SECONDS); //
				WritableFile writable2 = test2File.openWritable(1, TimeUnit.SECONDS)) {
			writable1.write(ByteBuffer.wrap("hello".getBytes()));
			writable2.write(ByteBuffer.wrap("world".getBytes()));
		}
		Assert.assertTrue(test1File.exists());
		Assert.assertTrue(test2File.exists());

		// copy foo/bar/ to qwe/asd/ (result is qwe/asd/bar/file1.txt & qwe/asd/bar/file2.txt)
		fooBarFolder.copyTo(qweAsdFolder);
		Assert.assertTrue(qweAsdBarFolder.exists());
		Assert.assertEquals(1, qweAsdFolder.folders().count());
		Assert.assertEquals(2, qweAsdBarFolder.files().count());

		// make sure original files still exist:
		Assert.assertTrue(test1File.exists());
		Assert.assertTrue(test2File.exists());
	}

}
