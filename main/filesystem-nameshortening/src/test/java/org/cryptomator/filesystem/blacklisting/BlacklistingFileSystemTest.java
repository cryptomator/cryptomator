/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
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
	public void testPreventCreationOfBlacklistedFolder() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Node blacklisted = underlyingFs.folder("qwe");
		final FileSystem fs = new BlacklistingFileSystem(underlyingFs, SamePathPredicate.forNode(blacklisted));
		fs.folder("qwe");
	}

	@Test(expected = UncheckedIOException.class)
	public void testPreventCreationOBlacklistedFile() {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Node blacklisted = underlyingFs.folder("qwe");
		final FileSystem fs = new BlacklistingFileSystem(underlyingFs, SamePathPredicate.forNode(blacklisted));
		fs.file("qwe");
	}

	@Test
	public void testBlacklistingOfFilesAndFolders() throws IOException {
		final FileSystem underlyingFs = new InMemoryFileSystem();
		final Folder hiddenFolder = underlyingFs.folder("asd");
		final File hiddenFile = underlyingFs.file("qwe");
		final Folder visibleFolder = underlyingFs.folder("sdf");
		final File visibleFile = underlyingFs.file("wer");
		final Predicate<Node> hiddenPredicate = SamePathPredicate.forNode(hiddenFolder).or(SamePathPredicate.forNode(hiddenFile));
		final FileSystem fs = new BlacklistingFileSystem(underlyingFs, hiddenPredicate);
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
		Assert.assertArrayEquals(new String[] {"sdf", "wer"}, fs.children().map(Node::name).sorted().collect(Collectors.toList()).toArray());
	}

}
