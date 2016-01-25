/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.jackrabbit;

import java.net.URI;
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileSystemResourceLocatorFactoryTest {

	private FileSystemResourceLocatorFactory factory;

	@Before
	public void setupLocatorFactory() {
		final FileSystem fs = new InMemoryFileSystem();
		fs.folder("existingFolder").create();
		try (WritableFile writable = fs.file("existingFile").openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}
		factory = new FileSystemResourceLocatorFactory(URI.create("http://localhost/contextroot"), fs);
	}

	@Test
	public void testCreateFolders() {
		FileSystemResourceLocator locator = factory.createResourceLocator(null, null, "/foo/bar/");
		Assert.assertTrue(locator instanceof FolderLocator);
		Assert.assertEquals("bar", locator.name());
		Assert.assertEquals("foo", locator.parent().get().name());
		Assert.assertEquals("", locator.parent().get().parent().get().name());
		Assert.assertFalse(locator.parent().get().parent().get().parent().isPresent());

		locator = factory.createResourceLocator("http://localhost/contextroot", "http://localhost/contextroot/foo/bar/");
		Assert.assertTrue(locator instanceof FolderLocator);
		Assert.assertEquals("bar", locator.name());
		Assert.assertEquals("foo", locator.parent().get().name());
		Assert.assertEquals("", locator.parent().get().parent().get().name());
		Assert.assertFalse(locator.parent().get().parent().get().parent().isPresent());
	}

	@Test
	public void testCreateFiles() {
		FileSystemResourceLocator locator = factory.createResourceLocator(null, null, "/foo/bar");
		Assert.assertTrue(locator instanceof FileLocator);
		Assert.assertEquals("bar", locator.name());
		Assert.assertEquals("foo", locator.parent().get().name());
		Assert.assertEquals("", locator.parent().get().parent().get().name());
		Assert.assertFalse(locator.parent().get().parent().get().parent().isPresent());

		locator = factory.createResourceLocator("http://localhost/contextroot", "http://localhost/contextroot/foo/bar");
		Assert.assertTrue(locator instanceof FileLocator);
		Assert.assertEquals("bar", locator.name());
		Assert.assertEquals("foo", locator.parent().get().name());
		Assert.assertEquals("", locator.parent().get().parent().get().name());
		Assert.assertFalse(locator.parent().get().parent().get().parent().isPresent());
	}

}
