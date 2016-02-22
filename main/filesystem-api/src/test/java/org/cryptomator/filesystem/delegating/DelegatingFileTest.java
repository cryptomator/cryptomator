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
import java.util.Optional;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegatingFileTest {

	@Test
	public void testName() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Mockito.when(mockFile.name()).thenReturn("Test");
		Assert.assertEquals(mockFile.name(), delegatingFile.name());
	}

	@Test
	public void testParent() {
		Folder mockFolder = Mockito.mock(Folder.class);
		File mockFile = Mockito.mock(File.class);

		TestDelegatingFileSystem delegatingParent = TestDelegatingFileSystem.withRoot(mockFolder);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(delegatingParent, mockFile);
		Assert.assertEquals(delegatingParent, delegatingFile.parent().get());
	}

	@Test
	public void testExists() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Mockito.when(mockFile.exists()).thenReturn(true);
		Assert.assertTrue(delegatingFile.exists());

		Mockito.when(mockFile.exists()).thenReturn(false);
		Assert.assertFalse(delegatingFile.exists());
	}

	@Test
	public void testLastModified() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Instant now = Instant.now();
		Mockito.when(mockFile.lastModified()).thenReturn(now);
		Assert.assertEquals(now, delegatingFile.lastModified());
	}

	@Test
	public void testSetLastModified() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Instant now = Instant.now();
		delegatingFile.setLastModified(now);
		Mockito.verify(mockFile).setLastModified(now);
	}

	@Test
	public void testCreationTime() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Instant now = Instant.now();
		Mockito.when(mockFile.creationTime()).thenReturn(Optional.of(now));
		Assert.assertEquals(now, delegatingFile.creationTime().get());
	}

	@Test
	public void testSetCreationTime() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		Instant now = Instant.now();
		delegatingFile.setCreationTime(now);
		Mockito.verify(mockFile).setCreationTime(now);
	}

	@Test
	public void testOpenReadable() {
		File mockFile = Mockito.mock(File.class);
		ReadableFile mockReadableFile = Mockito.mock(ReadableFile.class);

		Mockito.when(mockFile.openReadable()).thenReturn(mockReadableFile);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);
		Assert.assertNotNull(delegatingFile.openReadable());
	}

	@Test
	public void testOpenWritable() {
		File mockFile = Mockito.mock(File.class);
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);

		Mockito.when(mockFile.openWritable()).thenReturn(mockWritableFile);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);
		Assert.assertNotNull(delegatingFile.openWritable());
	}

	@Test
	public void testMoveTo() {
		File mockFile1 = Mockito.mock(File.class);
		File mockFile2 = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile1 = new TestDelegatingFile(null, mockFile1);
		DelegatingFile<?> delegatingFile2 = new TestDelegatingFile(null, mockFile2);

		delegatingFile1.moveTo(delegatingFile2);
		Mockito.verify(mockFile1).moveTo(mockFile2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMoveToDestinationFromDifferentLayer() {
		File mockFile1 = Mockito.mock(File.class);
		File mockFile2 = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile1 = new TestDelegatingFile(null, mockFile1);

		delegatingFile1.moveTo(mockFile2);
	}

	@Test
	public void testCopyTo() {
		File mockFile1 = Mockito.mock(File.class);
		File mockFile2 = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile1 = new TestDelegatingFile(null, mockFile1);
		DelegatingFile<?> delegatingFile2 = new TestDelegatingFile(null, mockFile2);

		delegatingFile1.copyTo(delegatingFile2);
		Mockito.verify(mockFile1).copyTo(mockFile2);
	}

	@Test
	public void testCopyToDestinationFromDifferentLayer() {
		File mockFile1 = Mockito.mock(File.class);
		File mockFile2 = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile1 = new TestDelegatingFile(null, mockFile1);

		delegatingFile1.copyTo(mockFile2);
		Mockito.verify(mockFile1).copyTo(mockFile2);
	}

	@Test
	public void testDelete() {
		File mockFile = Mockito.mock(File.class);
		DelegatingFile<?> delegatingFile = new TestDelegatingFile(null, mockFile);

		delegatingFile.delete();
		Mockito.verify(mockFile).delete();
	}

	@Test
	public void testCompareTo() {
		File mockFile1 = Mockito.mock(File.class);
		File mockFile2 = Mockito.mock(File.class);

		Mockito.when(mockFile1.compareTo(mockFile2)).thenReturn(-1);
		DelegatingFile<?> delegatingFile1 = new TestDelegatingFile(null, mockFile1);
		DelegatingFile<?> delegatingFile2 = new TestDelegatingFile(null, mockFile2);
		Assert.assertEquals(-1, delegatingFile1.compareTo(delegatingFile2));
	}

}
