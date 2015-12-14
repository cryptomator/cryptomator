package org.cryptomator.crypto;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class CryptoFileSystemTest {

	@Test
	public void testFilenameEncryption() throws UncheckedIOException, IOException {
		// some mock fs:
		FileSystem physicalFs = new InMemoryFileSystem();
		Folder dataRoot = physicalFs.folder("d");
		Assert.assertFalse(dataRoot.exists());

		// init crypto fs:
		FileSystem fs = new CryptoFileSystem(physicalFs);
		fs.create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(dataRoot.exists());
		Assert.assertEquals(physicalFs.children().count(), 2);
		Assert.assertEquals(1, dataRoot.files().count()); // ROOT file
		Assert.assertEquals(1, dataRoot.folders().count()); // ROOT directory

		// add another encrypted folder:
		Folder testFolder = fs.folder("test");
		Assert.assertFalse(testFolder.exists());
		testFolder.create(FolderCreateMode.INCLUDING_PARENTS);
		Assert.assertTrue(testFolder.exists());
		Assert.assertEquals(2, dataRoot.folders().count());
	}

}
