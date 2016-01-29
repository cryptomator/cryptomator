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

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class FilenameShortenerTest {

	@Test
	public void testNoDeflationOfShortFiles() {
		FileSystem fs = new InMemoryFileSystem();
		FilenameShortener shortener = new FilenameShortener(fs, 10);
		Assert.assertEquals("short", shortener.deflate("short"));
	}

	@Test
	public void testDeflateAndInflate() {
		FileSystem fs = new InMemoryFileSystem();
		FilenameShortener shortener = new FilenameShortener(fs, 10);

		String longName = "suchALongName";
		String shortenedName = shortener.deflate(longName);
		shortener.saveMapping(longName, shortenedName);
		Assert.assertNotEquals(longName, shortenedName);

		Assert.assertEquals(longName, shortener.inflate(shortenedName));
	}

	@Test
	public void testNoInflationOfShortFiles() {
		FileSystem fs = new InMemoryFileSystem();
		FilenameShortener shortener = new FilenameShortener(fs, 10);

		Assert.assertEquals("short", shortener.inflate("short"));
	}

	@Test(expected = UncheckedIOException.class)
	public void testInflateWithoutMappingFile() {
		FileSystem fs = new InMemoryFileSystem();
		FilenameShortener shortener = new FilenameShortener(fs, 10);

		shortener.inflate("iJustMadeThisNameUp.lng");
	}

}
