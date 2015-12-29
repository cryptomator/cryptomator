package org.cryptomator.filesystem.blacklisting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class BlacklistingFileSystemTest {

	@Test(expected = UncheckedIOException.class)
	public void testPreventCreationOfMetadataFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder metadataRoot = underlyingFs.folder("m");
		final Predicate<Node> metadataHidden = (Node n) -> n.equals(metadataRoot);
		final FileSystem fs = new BlacklistingFileSystem(underlyingFs, metadataHidden);
		fs.folder("m");
	}

	@Test
	public void testBlacklistingOfFilesAndFolders() throws IOException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder hiddenFolder = underlyingFs.folder("asd");
		final File hiddenFile = underlyingFs.file("qwe");
		final Folder visibleFolder = underlyingFs.folder("sdf");
		final File visibleFile = underlyingFs.file("wer");
		final Predicate<Node> metadataHidden = (Node n) -> n.equals(hiddenFolder) || n.equals(hiddenFile);
		final FileSystem fs = new BlacklistingFileSystem(underlyingFs, metadataHidden);
		hiddenFolder.create();
		try (WritableByteChannel writable = hiddenFile.openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}
		visibleFolder.create();
		try (WritableByteChannel writable = visibleFile.openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}

		Assert.assertArrayEquals(new String[] {"sdf"}, fs.folders().map(Node::name).collect(Collectors.toList()).toArray());
		Assert.assertArrayEquals(new String[] {"wer"}, fs.files().map(Node::name).collect(Collectors.toList()).toArray());
	}

}
