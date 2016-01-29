/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blacklisting;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Test;

public class SamePathPredicateTest {

	@Test
	public void testFileAndFolderWithSameNameWithSameParentConsideredSame() {
		FileSystem fs = new InMemoryFileSystem();
		File file1 = fs.file("foo");
		Folder folder1 = fs.folder("foo");
		Assert.assertTrue(SamePathPredicate.forNode(file1).test(folder1));
	}

	@Test
	public void testFilesWithDifferentParentConsideredDifferent() {
		FileSystem fs = new InMemoryFileSystem();
		File file1 = fs.file("foo");
		File file2 = fs.folder("bar").file("foo");
		Assert.assertFalse(SamePathPredicate.forNode(file1).test(file2));
	}

	@Test
	public void testFilesWithDifferentNamesConsideredDifferent() {
		FileSystem fs = new InMemoryFileSystem();
		File file1 = fs.file("foo");
		File file2 = fs.file("bar");
		Assert.assertFalse(SamePathPredicate.forNode(file1).test(file2));
	}

}
