package org.cryptomator.shortening;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class ShorteningFileSystemTest {

	@Test
	public void testCreationOfInvisibleMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		fs.folder("morethantenchars").create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
		Assert.assertTrue(metadataRoot.exists());
		Assert.assertEquals(1, fs.folders().count());
	}

	@Test(expected = UncheckedIOException.class)
	public void testPreventCreationOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		fs.folder("m");
	}

	@Test
	public void testDeflate() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		final Folder longNamedFolder = fs.folder("morethantenchars"); // base32(sha1(morethantenchars)) = QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP
		final File correspondingMetadataFile = metadataRoot.folder("QM").folder("JL").file("QMJL5GQUETRX2YRV6XDTJQ6NNM7IEUHP.lng");
		longNamedFolder.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
		Assert.assertTrue(longNamedFolder.exists());
		Assert.assertTrue(correspondingMetadataFile.exists());
	}

	@Test
	public void testDeflateAndInflateFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs1 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		final Folder longNamedFolder1 = fs1.folder("morethantenchars");
		longNamedFolder1.create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);

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
		fs1.folder("morethantenchars").create(FolderCreateMode.INCLUDING_PARENTS);
		try (WritableFile file = fs1.folder("morethantenchars").file("morethanelevenchars.txt").openWritable(1, TimeUnit.MILLISECONDS)) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}

		// read
		final FileSystem fs2 = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);
		try (ReadableFile file = fs2.folder("morethantenchars").file("morethanelevenchars.txt").openReadable(1, TimeUnit.MILLISECONDS)) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}
	}

	@Test
	public void testPassthroughShortNamedFiles() throws UncheckedIOException, TimeoutException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final FileSystem fs = new ShorteningFileSystem(underlyingFs, metadataRoot, 10);

		// of folders:
		underlyingFs.folder("foo").folder("bar").create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(fs.folder("foo").folder("bar").exists());

		// from underlying:
		try (WritableFile file = underlyingFs.folder("foo").file("test1.txt").openWritable(1, TimeUnit.MILLISECONDS)) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		try (ReadableFile file = fs.folder("foo").file("test1.txt").openReadable(1, TimeUnit.MILLISECONDS)) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}

		// to underlying:
		try (WritableFile file = fs.folder("foo").file("test2.txt").openWritable(1, TimeUnit.MILLISECONDS)) {
			file.write(ByteBuffer.wrap("hello world".getBytes()));
		}
		try (ReadableFile file = underlyingFs.folder("foo").file("test2.txt").openReadable(1, TimeUnit.MILLISECONDS)) {
			ByteBuffer buf = ByteBuffer.allocate(11);
			file.read(buf);
			Assert.assertEquals("hello world", new String(buf.array()));
		}
	}

}
