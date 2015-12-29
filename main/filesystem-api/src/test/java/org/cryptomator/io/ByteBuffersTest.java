/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.io;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class ByteBuffersTest {

	@Test
	public void testCopyOfEmptySource() {
		final ByteBuffer src = ByteBuffer.allocate(0);
		final ByteBuffer dst = ByteBuffer.allocate(5);
		dst.put(new byte[3]);
		Assert.assertEquals(0, src.position());
		Assert.assertEquals(0, src.remaining());
		Assert.assertEquals(3, dst.position());
		Assert.assertEquals(2, dst.remaining());
		ByteBuffers.copy(src, dst);
		Assert.assertEquals(0, src.position());
		Assert.assertEquals(0, src.remaining());
		Assert.assertEquals(3, dst.position());
		Assert.assertEquals(2, dst.remaining());
	}

	@Test
	public void testCopyToEmptyDestination() {
		final ByteBuffer src = ByteBuffer.wrap(new byte[4]);
		final ByteBuffer dst = ByteBuffer.allocate(0);
		src.put(new byte[2]);
		Assert.assertEquals(2, src.position());
		Assert.assertEquals(2, src.remaining());
		Assert.assertEquals(0, dst.position());
		Assert.assertEquals(0, dst.remaining());
		ByteBuffers.copy(src, dst);
		Assert.assertEquals(2, src.position());
		Assert.assertEquals(2, src.remaining());
		Assert.assertEquals(0, dst.position());
		Assert.assertEquals(0, dst.remaining());
	}

	@Test
	public void testCopyToBiggerDestination() {
		final ByteBuffer src = ByteBuffer.wrap(new byte[2]);
		final ByteBuffer dst = ByteBuffer.allocate(10);
		dst.put(new byte[3]);
		Assert.assertEquals(0, src.position());
		Assert.assertEquals(2, src.remaining());
		Assert.assertEquals(3, dst.position());
		Assert.assertEquals(7, dst.remaining());
		ByteBuffers.copy(src, dst);
		Assert.assertEquals(2, src.position());
		Assert.assertEquals(0, src.remaining());
		Assert.assertEquals(5, dst.position());
		Assert.assertEquals(5, dst.remaining());
	}

	@Test
	public void testCopyToSmallerDestination() {
		final ByteBuffer src = ByteBuffer.wrap(new byte[5]);
		final ByteBuffer dst = ByteBuffer.allocate(2);
		Assert.assertEquals(0, src.position());
		Assert.assertEquals(5, src.remaining());
		Assert.assertEquals(0, dst.position());
		Assert.assertEquals(2, dst.remaining());
		ByteBuffers.copy(src, dst);
		Assert.assertEquals(2, src.position());
		Assert.assertEquals(3, src.remaining());
		Assert.assertEquals(2, dst.position());
		Assert.assertEquals(0, dst.remaining());
	}

}
