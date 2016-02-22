/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;

import org.cryptomator.filesystem.WritableFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InMemoryFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreationTimeOfNonExistingFileThrowsUncheckedIOException() {
		InMemoryFileSystem fileSystem = new InMemoryFileSystem();
		InMemoryFile file = fileSystem.file("foo");

		thrown.expect(UncheckedIOException.class);

		file.creationTime();
	}

	@Test
	public void testCreationTimeOfCreatedFileIsSetToInstantDuringCreation() {
		InMemoryFileSystem fileSystem = new InMemoryFileSystem();
		InMemoryFile file = fileSystem.file("foo");

		Instant minCreationTime = Instant.now();
		Instant maxCreationTime;
		try (WritableFile writable = file.openWritable()) {
			maxCreationTime = Instant.now();
		}

		assertThat(file.creationTime().get().isBefore(minCreationTime), is(false));
		assertThat(file.creationTime().get().isAfter(maxCreationTime), is(false));
	}

	@Test
	public void testCreationTimeSetIsSaved() {
		Instant creationTime = Instant.parse("2015-03-23T21:11:32Z");
		InMemoryFileSystem fileSystem = new InMemoryFileSystem();
		InMemoryFile file = fileSystem.file("foo");
		try (WritableFile writable = file.openWritable()) {
			writable.write(ByteBuffer.allocate(0));
		}

		file.setCreationTime(creationTime);
		assertThat(file.creationTime().get(), is(creationTime));
	}

}
