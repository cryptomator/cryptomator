/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
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
		fooFolder.create();
		Assert.assertTrue(fooFolder.exists());
		Assert.assertEquals(1, fs.folders().count());

		// delete /foo
		fooFolder.delete();
		Assert.assertFalse(fooFolder.exists());
		Assert.assertEquals(0, fs.folders().count());

		// create /foo/bar
		Folder fooBarFolder = fooFolder.folder("bar");
		Assert.assertFalse(fooBarFolder.exists());
		fooBarFolder.create();
		Assert.assertTrue(fooFolder.exists());
		Assert.assertTrue(fooBarFolder.exists());
		Assert.assertEquals(1, fs.folders().count());
		Assert.assertEquals(1, fooFolder.folders().count());
	}

	@Test
	public void testImplicitUpdateOfModifiedDateAfterWrite() throws UncheckedIOException, TimeoutException, InterruptedException {
		final FileSystem fs = new InMemoryFileSystem();
		File fooFile = fs.file("foo.txt");

		final Instant beforeFirstModification = Instant.now();

		Thread.sleep(1);

		// write "hello world" to foo
		try (WritableFile writable = fooFile.openWritable()) {
			writable.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		Assert.assertTrue(fooFile.exists());
		final Instant firstModification = fooFile.lastModified();

		Thread.sleep(1);

		final Instant afterFirstModification = Instant.now();
		Assert.assertTrue(beforeFirstModification.isBefore(firstModification));
		Assert.assertTrue(afterFirstModification.isAfter(firstModification));

		Thread.sleep(1);

		// write "dlrow olleh" to foo
		try (WritableFile writable = fooFile.openWritable()) {
			writable.write(ByteBuffer.wrap("dlrow olleh".getBytes()));
		}
		Assert.assertTrue(fooFile.exists());
		final Instant secondModification = fooFile.lastModified();

		Assert.assertTrue(firstModification.isBefore(secondModification));
	}

	@Test
	public void testFileReadCopyMoveWrite() throws TimeoutException {
		final FileSystem fs = new InMemoryFileSystem();
		File fooFile = fs.file("foo.txt");

		// nothing happened yet:
		Assert.assertFalse(fooFile.exists());
		Assert.assertEquals(0, fs.files().count());

		// write "hello world" to foo
		try (WritableFile writable = fooFile.openWritable()) {
			writable.write(ByteBuffer.wrap("hello".getBytes()));
			writable.write(ByteBuffer.wrap(" ".getBytes()));
			writable.write(ByteBuffer.wrap("world".getBytes()));
		}
		Assert.assertTrue(fooFile.exists());

		// check if size = 11 bytes
		try (ReadableFile readable = fooFile.openReadable()) {
			Assert.assertEquals(11, readable.size());
		}

		// copy foo to bar
		File barFile = fs.file("bar.txt");
		fooFile.copyTo(barFile);
		Assert.assertTrue(fooFile.exists());
		Assert.assertTrue(barFile.exists());

		// move bar to baz
		File bazFile = fs.file("baz.txt");
		barFile.moveTo(bazFile);
		Assert.assertFalse(barFile.exists());
		Assert.assertTrue(bazFile.exists());

		// read "hello world" from baz
		final ByteBuffer readBuf1 = ByteBuffer.allocate(6);
		try (ReadableFile readable = bazFile.openReadable()) {
			readable.read(readBuf1);
			readBuf1.flip();
			Assert.assertEquals("hello ", new String(readBuf1.array(), 0, readBuf1.remaining()));
			readable.read(readBuf1);
			readBuf1.flip();
			Assert.assertEquals("world", new String(readBuf1.array(), 0, readBuf1.remaining()));
		}
		final ByteBuffer readBuf = ByteBuffer.allocate(5);
		try (ReadableFile readable = bazFile.openReadable()) {
			readable.position(6);
			readable.read(readBuf);
		}
		Assert.assertEquals("world", new String(readBuf.array()));
	}

	@Test
	public void testFolderCopy() throws TimeoutException {
		final FileSystem fs = new InMemoryFileSystem();
		final Folder fooBarFolder = fs.folder("foo").folder("bar");
		final Folder qweAsdFolder = fs.folder("qwe").folder("asd");
		final File test1File = fooBarFolder.file("test1.txt");
		final File test2File = fooBarFolder.file("test2.txt");
		fooBarFolder.create();

		// create some files inside foo/bar/
		try (WritableFile writable1 = test1File.openWritable(); //
				WritableFile writable2 = test2File.openWritable()) {
			writable1.write(ByteBuffer.wrap("hello".getBytes()));
			writable2.write(ByteBuffer.wrap("world".getBytes()));
		}
		Assert.assertTrue(test1File.exists());
		Assert.assertTrue(test2File.exists());

		// copy foo/bar/ to qwe/asd/ (result is qwe/asd/file1.txt &
		// qwe/asd/file2.txt)
		fooBarFolder.copyTo(qweAsdFolder);
		Assert.assertTrue(qweAsdFolder.exists());
		Assert.assertEquals(2, qweAsdFolder.files().count());

		// make sure original files still exist:
		Assert.assertTrue(test1File.exists());
		Assert.assertTrue(test2File.exists());
	}

}
