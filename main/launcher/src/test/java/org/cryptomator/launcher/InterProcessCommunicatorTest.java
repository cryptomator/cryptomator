/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class InterProcessCommunicatorTest {

	Path portFilePath = Mockito.mock(Path.class);
	Path portFileParentPath = Mockito.mock(Path.class);
	BasicFileAttributes portFileParentPathAttrs = Mockito.mock(BasicFileAttributes.class);
	FileSystem fs = Mockito.mock(FileSystem.class);
	FileSystemProvider provider = Mockito.mock(FileSystemProvider.class);
	SeekableByteChannel portFileChannel = Mockito.mock(SeekableByteChannel.class);
	AtomicInteger port = new AtomicInteger(-1);

	@Before
	public void setup() throws IOException {
		Mockito.when(portFilePath.getFileSystem()).thenReturn(fs);
		Mockito.when(portFilePath.toAbsolutePath()).thenReturn(portFilePath);
		Mockito.when(portFilePath.normalize()).thenReturn(portFilePath);
		Mockito.when(portFilePath.getParent()).thenReturn(portFileParentPath);
		Mockito.when(portFileParentPath.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.provider()).thenReturn(provider);
		Mockito.when(provider.readAttributes(portFileParentPath, BasicFileAttributes.class)).thenReturn(portFileParentPathAttrs);
		Mockito.when(portFileParentPathAttrs.isDirectory()).thenReturn(false, true); // Guava's MoreFiles will check if dir exists before attempting to create them.
		Mockito.when(provider.newByteChannel(Mockito.eq(portFilePath), Mockito.any(), Mockito.any())).thenReturn(portFileChannel);
		Mockito.when(portFileChannel.read(Mockito.any())).then(invocation -> {
			ByteBuffer buf = invocation.getArgument(0);
			buf.putInt(port.get());
			return Integer.BYTES;
		});
		Mockito.when(portFileChannel.write(Mockito.any())).then(invocation -> {
			ByteBuffer buf = invocation.getArgument(0);
			port.set(buf.getInt());
			return Integer.BYTES;
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testStartWithDummyPort1() throws IOException {
		port.set(0);
		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result.isServer());
			Mockito.verify(provider).createDirectory(portFileParentPath);
			Mockito.verifyZeroInteractions(protocol);
			result.handleLaunchArgs(new String[] {"foo"});
		}
	}

	@Test
	public void testStartWithDummyPort2() throws IOException {
		Mockito.doThrow(new NoSuchFileException("port file")).when(provider).checkAccess(portFilePath);

		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result.isServer());
			Mockito.verify(provider).createDirectory(portFileParentPath);
			Mockito.verifyZeroInteractions(protocol);
		}
	}

	@Test
	public void testInterProcessCommunication() throws IOException, InterruptedException {
		port.set(-1);
		InterProcessCommunicationProtocol protocol = Mockito.mock(InterProcessCommunicationProtocol.class);
		try (InterProcessCommunicator result1 = InterProcessCommunicator.start(portFilePath, protocol)) {
			Assert.assertTrue(result1.isServer());
			Mockito.verify(provider, Mockito.times(1)).createDirectory(portFileParentPath);
			Mockito.verifyZeroInteractions(protocol);

			try (InterProcessCommunicator result2 = InterProcessCommunicator.start(portFilePath, null)) {
				Assert.assertFalse(result2.isServer());
				Mockito.verify(provider, Mockito.times(1)).createDirectory(portFileParentPath);
				Assert.assertNotSame(result1, result2);

				result2.handleLaunchArgs(new String[] {"foo"});
				Mockito.verify(protocol).handleLaunchArgs(new String[] {"foo"});
			}
		}
	}

}
