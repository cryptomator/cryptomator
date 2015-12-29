/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.delegating;

import java.nio.ByteBuffer;
import java.time.Instant;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DelegatingWritableFileTest {

	@Test
	public void testIsOpen() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		Mockito.when(mockWritableFile.isOpen()).thenReturn(true);
		Assert.assertTrue(delegatingWritableFile.isOpen());

		Mockito.when(mockWritableFile.isOpen()).thenReturn(false);
		Assert.assertFalse(delegatingWritableFile.isOpen());
	}

	@Test
	public void testMoveTo() {
		WritableFile mockWritableFile1 = Mockito.mock(WritableFile.class);
		WritableFile mockWritableFile2 = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile1 = new DelegatingWritableFile(mockWritableFile1);
		DelegatingWritableFile delegatingWritableFile2 = new DelegatingWritableFile(mockWritableFile2);

		delegatingWritableFile1.moveTo(delegatingWritableFile2);
		Mockito.verify(mockWritableFile1).moveTo(mockWritableFile2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMoveToDestinationFromDifferentLayer() {
		WritableFile mockWritableFile1 = Mockito.mock(WritableFile.class);
		WritableFile mockWritableFile2 = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile1 = new DelegatingWritableFile(mockWritableFile1);

		delegatingWritableFile1.moveTo(mockWritableFile2);
	}

	@Test
	public void testSetLastModified() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		Instant now = Instant.now();
		delegatingWritableFile.setLastModified(now);
		Mockito.verify(mockWritableFile).setLastModified(now);
	}

	@Test
	public void testDelete() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		delegatingWritableFile.delete();
		Mockito.verify(mockWritableFile).delete();
	}

	@Test
	public void testTruncate() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		delegatingWritableFile.truncate();
		Mockito.verify(mockWritableFile).truncate();
	}

	@Test
	public void testWrite() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		ByteBuffer buf = ByteBuffer.allocate(4);
		Mockito.when(mockWritableFile.write(buf)).thenReturn(4);
		Assert.assertEquals(4, delegatingWritableFile.write(buf));
		Mockito.verify(mockWritableFile).write(buf);
	}

	@Test
	public void testPosition() {
		WritableFile mockWritableFile = Mockito.mock(WritableFile.class);
		@SuppressWarnings("resource")
		DelegatingWritableFile delegatingWritableFile = new DelegatingWritableFile(mockWritableFile);

		delegatingWritableFile.position(42);
		Mockito.verify(mockWritableFile).position(42);
	}

	@Test
	public void testClose() {
		ReadableFile mockReadableFile = Mockito.mock(ReadableFile.class);
		DelegatingReadableFile delegatingReadableFile = new DelegatingReadableFile(mockReadableFile);

		delegatingReadableFile.close();
		Mockito.verify(mockReadableFile).close();
	}

}
