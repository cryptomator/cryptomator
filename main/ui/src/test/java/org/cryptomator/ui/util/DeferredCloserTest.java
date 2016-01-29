/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.Closeable;

import org.junit.Test;

public class DeferredCloserTest {
	@Test
	public void testBasicFunctionality() throws Exception {
		DeferredCloser closer = new DeferredCloser();

		final Closeable obj = mock(Closeable.class);

		final DeferredClosable<Closeable> resource = closer.closeLater(obj);

		assertTrue(resource.get().isPresent());
		assertTrue(resource.get().get() == obj);

		closer.close();

		assertFalse(resource.get().isPresent());
		verify(obj).close();
	}

	@Test
	public void testAutoremoval() throws Exception {
		DeferredCloser closer = new DeferredCloser();

		final DeferredClosable<Closeable> resource = closer.closeLater(mock(Closeable.class));
		final DeferredClosable<Closeable> resource2 = closer.closeLater(mock(Closeable.class));

		resource.close();

		assertFalse(resource.get().isPresent());
		assertEquals(1, closer.cleanups.size());

		assertTrue(resource2.get().isPresent());

		closer.close();

		assertFalse(resource2.get().isPresent());

		assertEquals(0, closer.cleanups.size());
	}
}
