/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import org.cryptomator.ui.model.AppLaunchEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileOpenRequestHandlerTest {

	private FileOpenRequestHandler inTest;
	private BlockingQueue<AppLaunchEvent> queue;

	@BeforeEach
	public void setup() {
		queue = new ArrayBlockingQueue<>(1);
		inTest = new FileOpenRequestHandler(queue);
	}

	@Test
	@DisplayName("./cryptomator.exe foo bar")
	public void testOpenArgsWithCorrectPaths() throws IOException {
		inTest.handleLaunchArgs(new String[]{"foo", "bar"});

		AppLaunchEvent evt = queue.poll();
		Assertions.assertNotNull(evt);
		List<Path> paths = evt.getPathsToOpen().collect(Collectors.toList());
		MatcherAssert.assertThat(paths, CoreMatchers.hasItems(Paths.get("foo"), Paths.get("bar")));
	}

	@Test
	@DisplayName("./cryptomator.exe foo (with 'foo' being an invalid path)")
	public void testOpenArgsWithIncorrectPaths() throws IOException {
		FileSystem fs = Mockito.mock(FileSystem.class);
		Mockito.when(fs.getPath("foo")).thenThrow(new InvalidPathException("foo", "foo is not a path"));
		inTest.handleLaunchArgs(fs, new String[]{"foo"});

		AppLaunchEvent evt = queue.poll();
		Assertions.assertNotNull(evt);
		List<Path> paths = evt.getPathsToOpen().collect(Collectors.toList());
		Assertions.assertTrue(paths.isEmpty());
	}

	@Test
	@DisplayName("./cryptomator.exe foo (with full event queue)")
	public void testOpenArgsWithFullQueue() throws IOException {
		queue.add(new AppLaunchEvent(Stream.empty()));
		Assumptions.assumeTrue(queue.remainingCapacity() == 0);

		inTest.handleLaunchArgs(new String[]{"foo"});
	}

}
