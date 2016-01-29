/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import java.nio.ByteBuffer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ShorteningFileSystemFactoryTest {

	private static final ShorteningFileSystemFactory shorteningFsFactory = DaggerShorteningFileSystemTestComponent.create().shorteningFileSystemFactory();
	private static final String LONG_NAME = "aaaaabbbbbcccccdddddeeeeefffffggggghhhhhiiiiijjjjjkkkkklllll" //
			+ "mmmmmnnnnnooooopppppqqqqqrrrrrssssstttttuuuuuvvvvvwwwwwxxxxxyyyyyzzzzz" //
			+ "00000111112222233333444445555566666777778888899999";
	private static final String SHORTENED_FILE = "UYPJJ35VGP2JJ4YISC5S2XQLENLR5MVC.lng"; // base32(sha1(LONG_NAME)) + '.lng'
	private static final String CORRESPONDING_METADATA_FILE = "m/UY/PJ/UYPJJ35VGP2JJ4YISC5S2XQLENLR5MVC.lng";

	private FileSystem physicalFs;
	private FileSystem shortenedFs;

	@Before
	public void setupFileSystems() {
		physicalFs = new InMemoryFileSystem();
		shortenedFs = shorteningFsFactory.get(physicalFs);
		shortenedFs.create();
	}

	@Test
	public void testFileCreation() {
		File file = shortenedFs.file(LONG_NAME);
		Assert.assertFalse(file.exists());
		Assert.assertFalse(physicalFs.resolveFile(SHORTENED_FILE).exists());
		Assert.assertFalse(physicalFs.resolveFile(CORRESPONDING_METADATA_FILE).exists());
		try (WritableFile w = file.openWritable()) {
			w.write(ByteBuffer.allocate(0));
		}
		Assert.assertTrue(file.exists());
		Assert.assertTrue(physicalFs.resolveFile(SHORTENED_FILE).exists());
		Assert.assertTrue(physicalFs.resolveFile(CORRESPONDING_METADATA_FILE).exists());
	}

	@Test
	public void testFolderCreation() {
		Folder folder = shortenedFs.folder(LONG_NAME);
		Assert.assertFalse(folder.exists());
		Assert.assertFalse(physicalFs.resolveFolder(SHORTENED_FILE).exists());
		Assert.assertFalse(physicalFs.resolveFile(CORRESPONDING_METADATA_FILE).exists());
		folder.create();
		Assert.assertTrue(folder.exists());
		Assert.assertTrue(physicalFs.resolveFolder(SHORTENED_FILE).exists());
		Assert.assertTrue(physicalFs.resolveFile(CORRESPONDING_METADATA_FILE).exists());
	}

}
