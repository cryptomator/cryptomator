/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.charsets;

import java.text.Normalizer.Form;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class NormalizedNameFolderTest {

	private Folder delegate;
	private File delegateSubFileNfc;
	private File delegateSubFileNfd;
	private Folder delegateSubFolderNfc;
	private Folder delegateSubFolderNfd;

	@Before
	public void setup() {
		delegate = Mockito.mock(Folder.class);
		delegateSubFileNfc = Mockito.mock(File.class);
		delegateSubFileNfd = Mockito.mock(File.class);
		Mockito.when(delegate.file("\u00C5")).thenReturn(delegateSubFileNfc);
		Mockito.when(delegateSubFileNfc.name()).thenReturn("\u00C5");
		Mockito.when(delegate.file("\u0041\u030A")).thenReturn(delegateSubFileNfd);
		Mockito.when(delegateSubFileNfd.name()).thenReturn("\u0041\u030A");
		delegateSubFolderNfc = Mockito.mock(Folder.class);
		delegateSubFolderNfd = Mockito.mock(Folder.class);
		Mockito.when(delegate.folder("\u00F4")).thenReturn(delegateSubFolderNfc);
		Mockito.when(delegateSubFolderNfc.name()).thenReturn("\u00F4");
		Mockito.when(delegate.folder("\u006F\u0302")).thenReturn(delegateSubFolderNfd);
		Mockito.when(delegateSubFolderNfd.name()).thenReturn("\u006F\u0302");
	}

	@Test
	public void testDisplayNameInNfc() {
		Folder folder1 = new NormalizedNameFolder(null, delegateSubFolderNfc, Form.NFC);
		Folder folder2 = new NormalizedNameFolder(null, delegateSubFolderNfd, Form.NFC);
		Assert.assertEquals("\u00F4", folder1.name());
		Assert.assertEquals("\u00F4", folder2.name());
	}

	@Test
	public void testDisplayNameInNfd() {
		Folder folder1 = new NormalizedNameFolder(null, delegateSubFolderNfc, Form.NFD);
		Folder folder2 = new NormalizedNameFolder(null, delegateSubFolderNfd, Form.NFD);
		Assert.assertEquals("\u006F\u0302", folder1.name());
		Assert.assertEquals("\u006F\u0302", folder2.name());
	}

	@Test
	public void testNoFolderMigration1() {
		Mockito.when(delegateSubFolderNfc.exists()).thenReturn(true);
		Mockito.when(delegateSubFolderNfd.exists()).thenReturn(false);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		Folder sub1 = folder.folder("\u00F4");
		Folder sub2 = folder.folder("\u006F\u0302");
		Mockito.verify(delegateSubFolderNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testNoFolderMigration2() {
		Mockito.when(delegateSubFolderNfc.exists()).thenReturn(true);
		Mockito.when(delegateSubFolderNfd.exists()).thenReturn(true);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		Folder sub1 = folder.folder("\u00F4");
		Folder sub2 = folder.folder("\u006F\u0302");
		Mockito.verify(delegateSubFolderNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testNoFolderMigration3() {
		Mockito.when(delegateSubFolderNfc.exists()).thenReturn(false);
		Mockito.when(delegateSubFolderNfd.exists()).thenReturn(false);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		Folder sub1 = folder.folder("\u00F4");
		Folder sub2 = folder.folder("\u006F\u0302");
		Mockito.verify(delegateSubFolderNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testFolderMigration() {
		Mockito.when(delegateSubFolderNfc.exists()).thenReturn(false);
		Mockito.when(delegateSubFolderNfd.exists()).thenReturn(true);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		Folder sub1 = folder.folder("\u00F4");
		Mockito.verify(delegateSubFolderNfd).moveTo(delegateSubFolderNfc);
		Folder sub2 = folder.folder("\u006F\u0302");
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testNoFileMigration1() {
		Mockito.when(delegateSubFileNfc.exists()).thenReturn(true);
		Mockito.when(delegateSubFileNfd.exists()).thenReturn(false);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		File sub1 = folder.file("\u00C5");
		File sub2 = folder.file("\u0041\u030A");
		Mockito.verify(delegateSubFileNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testNoFileMigration2() {
		Mockito.when(delegateSubFileNfc.exists()).thenReturn(true);
		Mockito.when(delegateSubFileNfd.exists()).thenReturn(true);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		File sub1 = folder.file("\u00C5");
		File sub2 = folder.file("\u0041\u030A");
		Mockito.verify(delegateSubFileNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testNoFileMigration3() {
		Mockito.when(delegateSubFileNfc.exists()).thenReturn(false);
		Mockito.when(delegateSubFileNfd.exists()).thenReturn(false);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		File sub1 = folder.file("\u00C5");
		File sub2 = folder.file("\u0041\u030A");
		Mockito.verify(delegateSubFileNfd, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(sub1, sub2);
	}

	@Test
	public void testFileMigration() {
		Mockito.when(delegateSubFileNfc.exists()).thenReturn(false);
		Mockito.when(delegateSubFileNfd.exists()).thenReturn(true);
		Folder folder = new NormalizedNameFolder(null, delegate, Form.NFC);
		File sub1 = folder.file("\u00C5");
		Mockito.verify(delegateSubFileNfd).moveTo(delegateSubFileNfc);
		File sub2 = folder.file("\u0041\u030A");
		Assert.assertSame(sub1, sub2);
	}

}
