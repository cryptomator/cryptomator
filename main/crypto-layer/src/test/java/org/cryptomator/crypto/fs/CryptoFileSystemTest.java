/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.NoCryptor;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoFileSystemTest {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoFileSystemTest.class);

	@Test
	public void testFilenameEncryption() throws UncheckedIOException, IOException {
		// mock cryptor:
		Cryptor cryptor = new NoCryptor();

		// some mock fs:
		FileSystem physicalFs = new InMemoryFileSystem();
		Folder physicalDataRoot = physicalFs.folder("d");
		Assert.assertFalse(physicalDataRoot.exists());

		// init crypto fs:
		FileSystem fs = new CryptoFileSystem(physicalFs, cryptor);
		fs.create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(physicalDataRoot.exists());
		Assert.assertEquals(physicalFs.children().count(), 2);
		Assert.assertEquals(1, physicalDataRoot.files().count()); // ROOT file
		Assert.assertEquals(1, physicalDataRoot.folders().count()); // ROOT directory

		// add another encrypted folder:
		Folder fooFolder = fs.folder("foo");
		Folder barFolder = fooFolder.folder("bar");
		Assert.assertFalse(fooFolder.exists());
		Assert.assertFalse(barFolder.exists());
		barFolder.create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(fooFolder.exists());
		Assert.assertTrue(barFolder.exists());
		Assert.assertEquals(3, countDataFolders(physicalDataRoot)); // parent + foo + bar

		LOG.info(DirectoryPrinter.print(fs));
		LOG.info(DirectoryPrinter.print(physicalFs));
	}

	/**
	 * @return number of folders on second level inside the given dataRoot folder.
	 */
	private static int countDataFolders(Folder dataRoot) {
		final AtomicInteger num = new AtomicInteger();
		DirectoryWalker.walk(dataRoot, 0, 2, (node) -> {
			if (node instanceof Folder) {
				final Folder nodeParent = node.parent().get();
				final Folder nodeParentParent = nodeParent.parent().orElse(null);
				if (nodeParentParent != null && nodeParentParent.equals(dataRoot)) {
					num.incrementAndGet();
				}
			}
		});
		return num.get();
	}

}
