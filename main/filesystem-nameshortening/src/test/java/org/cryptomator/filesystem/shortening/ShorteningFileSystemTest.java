/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.contains;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.cryptomator.common.test.matcher.PropertyMatcher;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ShorteningFileSystemTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String METADATA_DIR_NAME = "m";
	private static final int THRESHOLD = 10;
	private static final String NAME_LONGER_THAN_THRESHOLD = "morethantenchars";

	@Test
	public void testImplicitCreationOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder(METADATA_DIR_NAME);
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		fs.folder(NAME_LONGER_THAN_THRESHOLD).create();
		Assert.assertTrue(metadataRoot.exists());
	}

	@Test
	public void testMetadataFolderIsNotIncludedInFolderListing() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		fs.folder(NAME_LONGER_THAN_THRESHOLD).create();

		assertThat(fs.folders().collect(toList()), contains(folderWithName(NAME_LONGER_THAN_THRESHOLD)));
	}

	@Test
	public void testMetadataFolderIsNotIncludedInChildrenListing() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		fs.folder(NAME_LONGER_THAN_THRESHOLD).create();

		assertThat(fs.children().collect(toList()), contains(folderWithName(NAME_LONGER_THAN_THRESHOLD)));
	}

	@Test
	public void testCanNotObtainFolderWithNameOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(format("'%s' is a reserved name", METADATA_DIR_NAME));

		fs.folder(METADATA_DIR_NAME);
	}

	@Test
	public void testCanNotObtainFileWithNameOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(format("'%s' is a reserved name", METADATA_DIR_NAME));

		fs.file(METADATA_DIR_NAME);
	}

	@Test
	public void testDeflate() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder(METADATA_DIR_NAME);
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, 10);
		final Folder longNamedFolder = fs.folder("morethantenchars"); // base32(sha1(morethantenchars)) = QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP
		final File correspondingMetadataFile = metadataRoot.folder("QM").folder("JL").file("QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP.lng");
		longNamedFolder.create();
		Assert.assertTrue(longNamedFolder.exists());
		Assert.assertTrue(correspondingMetadataFile.exists());
	}

	@Test
	public void testMoveLongFolders() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder(METADATA_DIR_NAME);
		metadataRoot.create();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);

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
		final Folder metadataRoot = underlyingFs.folder(METADATA_DIR_NAME);
		metadataRoot.create();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);

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
		final FileSystem fs1 = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		final Folder longNamedFolder1 = fs1.folder("morethantenchars");
		longNamedFolder1.create();

		final FileSystem fs2 = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		final Folder longNamedFolder2 = fs2.folder("morethantenchars");
		Assert.assertTrue(longNamedFolder2.exists());
	}

	@Test
	public void testDeflateAndInflateFolderAndFile() throws UncheckedIOException, TimeoutException {
		final FileSystem underlyingFs = new InMemoryFileSystem();

		// write:
		final FileSystem fs1 = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		fs1.folder("morethantenchars").create();
		try (WritableFile file = fs1.folder("morethantenchars").file("morethanelevenchars.txt").openWritable()) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}

		// read
		final FileSystem fs2 = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);
		try (ReadableFile file = fs2.folder("morethantenchars").file("morethanelevenchars.txt").openReadable()) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}
	}

	@Test
	public void testPassthroughShortNamedFiles() throws UncheckedIOException, TimeoutException, InterruptedException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, METADATA_DIR_NAME, THRESHOLD);

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

	public static Matcher<Folder> folderWithName(String name) {
		return new PropertyMatcher<>(Folder.class, Folder::name, "name", is(name));
	}

}
