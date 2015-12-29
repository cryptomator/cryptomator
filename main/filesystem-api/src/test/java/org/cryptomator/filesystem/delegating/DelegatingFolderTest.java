/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.delegating;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegatingFolderTest {

	@Test
	public void testName() {
		Folder mockFolder = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder = new DelegatingFolder(null, mockFolder, null, null);

		Mockito.when(mockFolder.name()).thenReturn("Test");
		Assert.assertEquals(mockFolder.name(), delegatingFolder.name());
	}

	@Test
	public void testParent() {
		Folder mockFolder1 = Mockito.mock(Folder.class);
		Folder mockFolder2 = Mockito.mock(Folder.class);

		DelegatingFolder delegatingParent = DelegatingFileSystem.withDelegate(mockFolder1);
		DelegatingFolder delegatingFolder = new DelegatingFolder(delegatingParent, mockFolder2, null, null);
		Assert.assertEquals(delegatingParent, delegatingFolder.parent().get());
	}

	@Test
	public void testExists() {
		Folder mockFolder = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder = new DelegatingFolder(null, mockFolder, null, null);

		Mockito.when(mockFolder.exists()).thenReturn(true);
		Assert.assertTrue(delegatingFolder.exists());

		Mockito.when(mockFolder.exists()).thenReturn(false);
		Assert.assertFalse(delegatingFolder.exists());
	}

	@Test
	public void testLastModified() {
		Folder mockFolder = Mockito.mock(Folder.class);
		Instant now = Instant.now();

		Mockito.when(mockFolder.lastModified()).thenReturn(now);
		DelegatingFolder delegatingFolder = new DelegatingFolder(null, mockFolder, null, null);
		Assert.assertEquals(now, delegatingFolder.lastModified());
	}

	@Test
	public void testChildren() {
		Folder mockFolder = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder = DelegatingFileSystem.withDelegate(mockFolder);

		Folder subFolder1 = Mockito.mock(Folder.class);
		DelegatingFolder delegatingSubFolder1 = new DelegatingFolder(delegatingFolder, subFolder1, null, null);
		File subFile1 = Mockito.mock(File.class);
		DelegatingFile delegatingSubFile1 = new DelegatingFile(delegatingFolder, subFile1, null, null);

		/* folders */
		Mockito.when(mockFolder.folder("subFolder1")).thenReturn(subFolder1);
		Assert.assertEquals(delegatingSubFolder1, delegatingFolder.folder("subFolder1"));

		Mockito.<Stream<? extends Folder>>when(mockFolder.folders()).thenAnswer((invocation) -> {
			return Arrays.stream(new Folder[] {subFolder1});
		});
		List<DelegatingFolder> subFolders = delegatingFolder.folders().collect(Collectors.toList());
		Assert.assertThat(subFolders, Matchers.containsInAnyOrder(delegatingSubFolder1));

		/* files */
		Mockito.when(mockFolder.file("subFile1")).thenReturn(subFile1);
		Assert.assertEquals(delegatingSubFile1, delegatingFolder.file("subFile1"));

		Mockito.<Stream<? extends File>>when(mockFolder.files()).thenAnswer((invocation) -> {
			return Arrays.stream(new File[] {subFile1});
		});
		List<DelegatingFile> subFiles = delegatingFolder.files().collect(Collectors.toList());
		Assert.assertThat(subFiles, Matchers.containsInAnyOrder(delegatingSubFile1));

		/* files and folders */
		List<DelegatingNode<?>> children = delegatingFolder.children().collect(Collectors.toList());
		DelegatingNode<?>[] expectedChildren = new DelegatingNode[] {delegatingSubFolder1, delegatingSubFile1};
		Assert.assertThat(children, Matchers.containsInAnyOrder(expectedChildren));

	}

	@Test
	public void testMoveTo() {
		Folder mockFolder1 = Mockito.mock(Folder.class);
		Folder mockFolder2 = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder1 = new DelegatingFolder(null, mockFolder1, null, null);
		DelegatingFolder delegatingFolder2 = new DelegatingFolder(null, mockFolder2, null, null);

		delegatingFolder1.moveTo(delegatingFolder2);
		Mockito.verify(mockFolder1).moveTo(mockFolder2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMoveToDestinationFromDifferentLayer() {
		Folder mockFolder1 = Mockito.mock(Folder.class);
		Folder mockFolder2 = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder1 = new DelegatingFolder(null, mockFolder1, null, null);

		delegatingFolder1.moveTo(mockFolder2);
	}

	@Test
	public void testCopyTo() {
		Folder mockFolder1 = Mockito.mock(Folder.class);
		Folder mockFolder2 = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder1 = new DelegatingFolder(null, mockFolder1, null, null);
		DelegatingFolder delegatingFolder2 = new DelegatingFolder(null, mockFolder2, null, null);

		delegatingFolder1.copyTo(delegatingFolder2);
		Mockito.verify(mockFolder1).copyTo(mockFolder2);
	}

	@Test
	public void testCopyToDestinationFromDifferentLayer() {
		Folder mockFolder1 = Mockito.mock(Folder.class);
		Folder mockFolder2 = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder1 = new DelegatingFolder(null, mockFolder1, null, null);

		delegatingFolder1.copyTo(mockFolder2);
		Mockito.verify(mockFolder1).copyTo(mockFolder2);
	}

	@Test
	public void testCreate() {
		Folder mockFolder = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder = new DelegatingFolder(null, mockFolder, null, null);

		delegatingFolder.create();
		Mockito.verify(mockFolder).create();
	}

	@Test
	public void testDelete() {
		Folder mockFolder = Mockito.mock(Folder.class);
		DelegatingFolder delegatingFolder = new DelegatingFolder(null, mockFolder, null, null);

		delegatingFolder.delete();
		Mockito.verify(mockFolder).delete();
	}

}
