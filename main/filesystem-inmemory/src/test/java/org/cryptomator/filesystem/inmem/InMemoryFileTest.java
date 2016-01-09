package org.cryptomator.filesystem.inmem;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.UncheckedIOException;
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
		InMemoryFile inTest = fileSystem.file("foo");

		thrown.expect(UncheckedIOException.class);

		inTest.creationTime();
	}

	@Test
	public void testCreationTimeOfCreatedFileIsSetToInstantDuringCreation() {
		InMemoryFileSystem fileSystem = new InMemoryFileSystem();
		InMemoryFile inTest = fileSystem.file("foo");

		Instant minCreationTime = Instant.now();
		Instant maxCreationTime;
		try (WritableFile writable = inTest.openWritable()) {
			maxCreationTime = Instant.now();
		}

		assertThat(inTest.creationTime().get().isBefore(minCreationTime), is(false));
		assertThat(inTest.creationTime().get().isAfter(maxCreationTime), is(false));
	}

	@Test
	public void testCreationTimeSetInWritableFileIsSaved() {
		Instant creationTime = Instant.parse("2015-03-23T21:11:32Z");
		InMemoryFileSystem fileSystem = new InMemoryFileSystem();
		InMemoryFile inTest = fileSystem.file("foo");
		try (WritableFile writable = inTest.openWritable()) {
			writable.setCreationTime(creationTime);
		}

		assertThat(inTest.creationTime().get(), is(creationTime));
	}

}
