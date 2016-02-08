/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.filesystem.FileSystemVisitor.fileSystemVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.NoCryptor;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class CryptoFileSystemTest {

	@Test(timeout = 1000)
	public void testDirectoryCreation() throws UncheckedIOException, IOException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final Folder physicalDataRoot = physicalFs.folder("d");
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();

		// add another encrypted folder:
		final Folder fooFolder = fs.folder("foo");
		final Folder fooBarFolder = fooFolder.folder("bar");
		Assert.assertFalse(fooFolder.exists());
		Assert.assertFalse(fooBarFolder.exists());
		fooBarFolder.create();
		Assert.assertTrue(fooFolder.exists());
		Assert.assertTrue(fooBarFolder.exists());
		Assert.assertEquals(3, countDataFolders(physicalDataRoot)); // parent +
																	// foo + bar
	}

	@Test(timeout = 1000)
	public void testDirectoryMoving() throws UncheckedIOException, IOException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();

		// create foo/bar/ and then move foo/ to baz/:
		final Folder fooFolder = fs.folder("foo");
		final Folder fooBarFolder = fooFolder.folder("bar");
		final Folder bazFolder = fs.folder("baz");
		final Folder bazBarFolder = bazFolder.folder("bar");
		fooBarFolder.create();
		Assert.assertTrue(fooBarFolder.exists());
		Assert.assertFalse(bazFolder.exists());
		fooFolder.moveTo(bazFolder);
		// foo/bar/ should no longer exist, but baz/bar/ should:
		Assert.assertFalse(fooBarFolder.exists());
		Assert.assertTrue(bazFolder.exists());
		Assert.assertTrue(bazBarFolder.exists());
	}

	@Test(timeout = 1000, expected = UnsupportedOperationException.class)
	public void testMovingOfRootDir() throws UncheckedIOException, IOException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();
		fs.moveTo(fs.folder("subFolder"));
	}

	@Test(timeout = 1000, expected = UnsupportedOperationException.class)
	public void testDeletingOfRootDir() throws UncheckedIOException, IOException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();
		fs.delete();
	}

	@Test(timeout = 100000)
	public void testCreationAndLastModifiedDateOfRootDir() throws UncheckedIOException, IOException, InterruptedException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();

		final Instant minDate = Instant.now();
		Thread.sleep(10);
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		Thread.sleep(10);
		final Instant maxDate = Instant.now();

		Assert.assertTrue(fs.creationTime().isPresent());
		Assert.assertTrue(fs.creationTime().get().isAfter(minDate));
		Assert.assertTrue(fs.creationTime().get().isBefore(maxDate));
		Assert.assertTrue(fs.lastModified().isAfter(minDate));
		Assert.assertTrue(fs.lastModified().isBefore(maxDate));
	}

	@Test(timeout = 1000, expected = IllegalArgumentException.class)
	public void testDirectoryMovingWithinBloodline() throws UncheckedIOException, IOException {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();

		// create foo/bar/ and then try to move foo/bar/ to foo/
		final Folder fooFolder = fs.folder("foo");
		final Folder fooBarFolder = fooFolder.folder("bar");
		fooBarFolder.create();
		fooBarFolder.moveTo(fooFolder);
	}

	@Test(timeout = 10000)
	public void testWriteAndReadEncryptedFile() {
		// mock stuff and prepare crypto FS:
		final Cryptor cryptor = new NoCryptor();
		final FileSystem physicalFs = new InMemoryFileSystem();
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		fs.create();

		// write test content to file
		try (WritableFile writable = fs.file("test1.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("Hello World".getBytes()));
		}

		// read test content from file
		try (ReadableFile readable = fs.file("test1.txt").openReadable()) {
			ByteBuffer buf1 = ByteBuffer.allocate(5);
			readable.read(buf1);
			buf1.flip();
			Assert.assertEquals("Hello", new String(buf1.array(), 0, buf1.remaining()));
			ByteBuffer buf2 = ByteBuffer.allocate(10);
			readable.read(buf2);
			buf2.flip();
			Assert.assertArrayEquals(" World".getBytes(), Arrays.copyOfRange(buf2.array(), 0, buf2.remaining()));
		}
	}

	/**
	 * @return number of folders on second level inside the given dataRoot
	 *         folder.
	 */
	private static int countDataFolders(Folder dataRoot) {
		final AtomicInteger num = new AtomicInteger();
		fileSystemVisitor() //
				.afterFolder(folder -> {
					final Folder parent = folder.parent().get();
					final Folder parentOfParent = parent.parent().orElse(null);
					if (parentOfParent != null && parentOfParent.equals(dataRoot)) {
						num.incrementAndGet();
					}
				}) //
				.withMaxDepth(2) //
				.visit(dataRoot);
		return num.get();
	}

}
