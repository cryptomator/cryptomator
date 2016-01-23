/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
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
import org.cryptomator.filesystem.File;
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
	public void testVaultStructureInitialization() throws UncheckedIOException, IOException {
		// mock cryptor:
		final Cryptor cryptor = new NoCryptor();

		// some mock fs:
		final FileSystem physicalFs = new InMemoryFileSystem();
		final File masterkeyFile = physicalFs.file("masterkey.cryptomator");
		final File masterkeyBkupFile = physicalFs.file("masterkey.cryptomator.bkup");
		final Folder physicalDataRoot = physicalFs.folder("d");
		Assert.assertFalse(masterkeyFile.exists());
		Assert.assertFalse(masterkeyBkupFile.exists());
		Assert.assertFalse(physicalDataRoot.exists());

		// init crypto fs:
		final FileSystem fs = new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		Assert.assertTrue(masterkeyFile.exists());
		Assert.assertTrue(masterkeyBkupFile.exists());
		fs.create();
		Assert.assertTrue(physicalDataRoot.exists());
		Assert.assertEquals(3, physicalFs.children().count()); // d + masterkey.cryptomator + masterkey.cryptomator.bkup
		Assert.assertEquals(1, physicalDataRoot.files().count()); // ROOT file
		Assert.assertEquals(1, physicalDataRoot.folders().count()); // ROOT directory
	}

	@Test(timeout = 1000)
	public void testMasterkeyBackupBehaviour() throws InterruptedException {
		// mock cryptor:
		final Cryptor cryptor = new NoCryptor();

		// some mock fs:
		final FileSystem physicalFs = new InMemoryFileSystem();
		final File masterkeyBkupFile = physicalFs.file("masterkey.cryptomator.bkup");
		Assert.assertFalse(masterkeyBkupFile.exists());

		// first initialization:
		new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		Assert.assertTrue(masterkeyBkupFile.exists());
		final Instant bkupDateT0 = masterkeyBkupFile.lastModified();

		// make sure some time passes, as the resolution of last modified date
		// is not in nanos:
		Thread.sleep(1);

		// second initialization:
		new CryptoFileSystem(physicalFs, cryptor, Mockito.mock(CryptoFileSystemDelegate.class), "foo");
		Assert.assertTrue(masterkeyBkupFile.exists());
		final Instant bkupDateT1 = masterkeyBkupFile.lastModified();

		Assert.assertTrue(bkupDateT1.isAfter(bkupDateT0));
	}

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
