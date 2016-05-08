/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.charsets;

import java.nio.ByteBuffer;
import java.text.Normalizer.Form;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class NormalizedNameFileSystemTest {

	@Test
	public void testFileMigration() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		try (WritableFile writable = inMemoryFs.file("\u006F\u0302").openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFC);
		Assert.assertTrue(normalizationFs.file("\u00F4").exists());
		Assert.assertTrue(normalizationFs.file("\u006F\u0302").exists());
		Assert.assertFalse(inMemoryFs.file("\u006F\u0302").exists());
		Assert.assertTrue(inMemoryFs.file("\u00F4").exists());
	}

	@Test
	public void testNoFileMigration() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		try (WritableFile writable = inMemoryFs.file("\u00F4").openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFC);
		Assert.assertTrue(normalizationFs.file("\u00F4").exists());
		Assert.assertTrue(normalizationFs.file("\u006F\u0302").exists());
		Assert.assertFalse(inMemoryFs.file("\u006F\u0302").exists());
		Assert.assertTrue(inMemoryFs.file("\u00F4").exists());
	}

	@Test
	public void testFolderMigration() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		inMemoryFs.folder("\u006F\u0302").create();
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFC);
		Assert.assertTrue(normalizationFs.folder("\u00F4").exists());
		Assert.assertTrue(normalizationFs.folder("\u006F\u0302").exists());
		Assert.assertFalse(inMemoryFs.folder("\u006F\u0302").exists());
		Assert.assertTrue(inMemoryFs.folder("\u00F4").exists());
	}

	@Test
	public void testNoFolderMigration() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		inMemoryFs.folder("\u00F4").create();
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFC);
		Assert.assertTrue(normalizationFs.folder("\u00F4").exists());
		Assert.assertTrue(normalizationFs.folder("\u006F\u0302").exists());
		Assert.assertFalse(inMemoryFs.folder("\u006F\u0302").exists());
		Assert.assertTrue(inMemoryFs.folder("\u00F4").exists());
	}

	@Test
	public void testNfcDisplayNames() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		inMemoryFs.folder("a\u00F4").create();
		inMemoryFs.folder("b\u006F\u0302").create();
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFC);
		Assert.assertEquals("a\u00F4", normalizationFs.folder("a\u00F4").name());
		Assert.assertEquals("b\u00F4", normalizationFs.folder("b\u006F\u0302").name());
	}

	@Test
	public void testNfdDisplayNames() {
		FileSystem inMemoryFs = new InMemoryFileSystem();
		inMemoryFs.folder("a\u00F4").create();
		inMemoryFs.folder("b\u006F\u0302").create();
		FileSystem normalizationFs = new NormalizedNameFileSystem(inMemoryFs, Form.NFD);
		Assert.assertEquals("a\u006F\u0302", normalizationFs.folder("a\u00F4").name());
		Assert.assertEquals("b\u006F\u0302", normalizationFs.folder("b\u006F\u0302").name());
	}

}
