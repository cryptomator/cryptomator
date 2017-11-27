/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FileOpenRequestHandlerTest {

	@Test
	public void testOpenArgsWithCorrectPaths() throws IOException {
		Path p1 = Mockito.mock(Path.class);
		Path p2 = Mockito.mock(Path.class);
		FileSystem fs = Mockito.mock(FileSystem.class);
		FileSystemProvider provider = Mockito.mock(FileSystemProvider.class);
		BasicFileAttributes attrs = Mockito.mock(BasicFileAttributes.class);
		Mockito.when(p1.getFileSystem()).thenReturn(fs);
		Mockito.when(p2.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.provider()).thenReturn(provider);
		Mockito.when(fs.getPath(Mockito.anyString())).thenReturn(p1, p2);
		Mockito.when(provider.readAttributes(Mockito.any(), Mockito.eq(BasicFileAttributes.class))).thenReturn(attrs);

		BlockingQueue<Path> queue = new ArrayBlockingQueue<>(10);
		FileOpenRequestHandler handler = new FileOpenRequestHandler(queue);
		handler.handleLaunchArgs(fs, new String[] {"foo", "bar"});

		Assert.assertEquals(p1, queue.poll());
		Assert.assertEquals(p2, queue.poll());
	}

	@Test
	public void testOpenArgsWithIncorrectPaths() throws IOException {
		FileSystem fs = Mockito.mock(FileSystem.class);
		Mockito.when(fs.getPath(Mockito.anyString())).thenThrow(new InvalidPathException("foo", "foo is not a path"));

		@SuppressWarnings("unchecked")
		BlockingQueue<Path> queue = Mockito.mock(BlockingQueue.class);
		FileOpenRequestHandler handler = new FileOpenRequestHandler(queue);
		handler.handleLaunchArgs(fs, new String[] {"foo"});

		Mockito.verifyNoMoreInteractions(queue);
	}

	@Test
	public void testOpenArgsWithFullQueue() throws IOException {
		Path p = Mockito.mock(Path.class);
		FileSystem fs = Mockito.mock(FileSystem.class);
		FileSystemProvider provider = Mockito.mock(FileSystemProvider.class);
		BasicFileAttributes attrs = Mockito.mock(BasicFileAttributes.class);
		Mockito.when(p.getFileSystem()).thenReturn(fs);
		Mockito.when(fs.provider()).thenReturn(provider);
		Mockito.when(fs.getPath(Mockito.anyString())).thenReturn(p);
		Mockito.when(provider.readAttributes(Mockito.eq(p), Mockito.eq(BasicFileAttributes.class))).thenReturn(attrs);
		Mockito.when(attrs.isRegularFile()).thenReturn(true);

		@SuppressWarnings("unchecked")
		BlockingQueue<Path> queue = Mockito.mock(BlockingQueue.class);
		Mockito.when(queue.offer(Mockito.any())).thenReturn(false);
		FileOpenRequestHandler handler = new FileOpenRequestHandler(queue);
		handler.handleLaunchArgs(fs, new String[] {"foo"});
	}

}
