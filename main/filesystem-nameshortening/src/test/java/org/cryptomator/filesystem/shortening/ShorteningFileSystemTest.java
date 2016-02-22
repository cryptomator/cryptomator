/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class ShorteningFileSystemTest {

	@Test
	public void testImplicitCreationOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		fs.folder("morethantenchars").create();
		Assert.assertTrue(metadataRoot.exists());
	}

	@Test
	public void testDeflate() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		final Folder longNamedFolder = fs.folder("morethantenchars"); // base32(sha1(morethantenchars)) = QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP
		final File correspondingMetadataFile = metadataRoot.folder("QM").folder("JL").file("QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP.lng");
		longNamedFolder.create();
		Assert.assertTrue(longNamedFolder.exists());
		Assert.assertTrue(correspondingMetadataFile.exists());
	}

	@Test
	public void testMoveLongFolders() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		metadataRoot.create();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);

		final Folder shortNamedFolder = fs.folder("test");
		shortNamedFolder.create();
		Assert.assertFalse(metadataRoot.children().findAny().isPresent());

		final Folder longNamedFolder = fs.folder("morethantenchars");
		shortNamedFolder.moveTo(longNamedFolder);
		Assert.assertTrue(metadataRoot.children().findAny().isPresent());
	}

	@Test
	public void testMoveLongFiles() throws UncheckedIOException, TimeoutException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		metadataRoot.create();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);

		final File shortNamedFolder = fs.file("test");
		try (WritableFile file = shortNamedFolder.openWritable()) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		Assert.assertFalse(metadataRoot.children().findAny().isPresent());

		final File longNamedFolder = fs.file("morethantenchars");
		shortNamedFolder.moveTo(longNamedFolder);
		Assert.assertTrue(metadataRoot.children().findAny().isPresent());
	}

	@Test
	public void testDeflateAndInflateFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs1 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		final Folder longNamedFolder1 = fs1.folder("morethantenchars");
		longNamedFolder1.create();

		final FileSystem fs2 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		final Folder longNamedFolder2 = fs2.folder("morethantenchars");
		Assert.assertTrue(longNamedFolder2.exists());
	}

	@Test
	public void testDeflateAndInflateFolderAndFile() throws UncheckedIOException, TimeoutException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");

		// write:
		final FileSystem fs1 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		fs1.folder("morethantenchars").create();
		try (WritableFile file = fs1.folder("morethantenchars").file("morethanelevenchars.txt").openWritable()) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}

		// read
		final FileSystem fs2 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		try (ReadableFile file = fs2.folder("morethantenchars").file("morethanelevenchars.txt").openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}
	}

	@Test
	public void testPassthroughShortNamedFiles() throws UncheckedIOException, TimeoutException, InterruptedException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);

		final Instant testStart = Instant.now();

		Thread.sleep(1);

		// of folders:
		underlyingFs.folder("foo").folder("bar").create();
		Assert.assertTrue(fs.folder("foo").folder("bar").exists());

		// from underlying:
		try (WritableFile file = underlyingFs.folder("foo").file("test1.txt").openWritable()) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		try (ReadableFile file = fs.folder("foo").file("test1.txt").openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			buf.flip();
			Assert.assertEquals("hello world", new String(buf.array()));
		}
		Assert.assertTrue(fs.folder("foo").file("test1.txt").lastModified().isAfter(testStart));

		// to underlying:
		try (WritableFile file = fs.folder("foo").file("test2.txt").openWritable()) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		try (ReadableFile file = underlyingFs.folder("foo").file("test2.txt").openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}
		Assert.assertTrue(fs.folder("foo").file("test2.txt").lastModified().isAfter(testStart));
	}

}
