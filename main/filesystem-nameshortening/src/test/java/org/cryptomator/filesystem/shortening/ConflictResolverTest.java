package org.cryptomator.filesystem.shortening;

import java.util.Optional;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConflictResolverTest {

	private Folder metadataFolder;
	private FilenameShortener shortener;
	private Folder folder;
	private File canonicalFile;
	private File conflictingFile;
	private File resolvedFile;

	@Before
	public void setup() {
		metadataFolder = new InMemoryFileSystem();
		shortener = new FilenameShortener(metadataFolder, 20);
		folder = Mockito.mock(Folder.class);
		canonicalFile = Mockito.mock(File.class);
		conflictingFile = Mockito.mock(File.class);
		resolvedFile = Mockito.mock(File.class);

		String longName = "hello world, I am a very long file name. certainly longer than twenty characters.exe";
		String shortName = shortener.deflate(longName);
		shortener.saveMapping(longName, shortName);
		String canonicalFileName = shortName;
		String conflictingFileName = shortName.replace(".lng", " (1).lng");

		Mockito.when(canonicalFile.name()).thenReturn(canonicalFileName);
		Mockito.when(conflictingFile.name()).thenReturn(conflictingFileName);

		Mockito.when(canonicalFile.exists()).thenReturn(true);
		Mockito.when(conflictingFile.exists()).thenReturn(true);

		Mockito.doReturn(Optional.of(folder)).when(canonicalFile).parent();
		Mockito.doReturn(Optional.of(folder)).when(conflictingFile).parent();

		Mockito.when(folder.file(Mockito.anyString())).thenReturn(resolvedFile);
		Mockito.when(folder.file(canonicalFileName)).thenReturn(canonicalFile);
		Mockito.when(folder.file(conflictingFileName)).thenReturn(conflictingFile);
	}

	@Test
	public void testNoConflictToBeResolved() {
		File resolved = ConflictResolver.resolveConflictIfNecessary(canonicalFile, new FilenameShortener(metadataFolder, 20));
		Mockito.verify(conflictingFile, Mockito.never()).moveTo(Mockito.any());
		Assert.assertSame(canonicalFile, resolved);
	}

	@Test
	public void testConflictToBeResolved() {
		File resolved = ConflictResolver.resolveConflictIfNecessary(conflictingFile, new FilenameShortener(metadataFolder, 20));
		Mockito.verify(conflictingFile).moveTo(resolvedFile);
		Assert.assertSame(resolvedFile, resolved);
	}

}
