package org.cryptomator.filesystem.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConflictResolverTest {

	private ConflictResolver conflictResolver;
	private Folder folder;
	private File canonicalFile;
	private File canonicalFolder;
	private File conflictingFile;
	private File conflictingFolder;
	private File resolved;
	private File unrelatedFile;

	@Before
	public void setup() {
		Pattern base32Pattern = Pattern.compile("([A-Z0-9]{8})*[A-Z0-9=]{8}");
		BaseNCodec base32 = new Base32();
		Function<String, Optional<String>> decode = (s) -> Optional.of(new String(base32.decode(s), StandardCharsets.UTF_8));
		Function<String, Optional<String>> encode = (s) -> Optional.of(base32.encodeAsString(s.getBytes(StandardCharsets.UTF_8)));
		conflictResolver = new ConflictResolver(base32Pattern, decode, encode);

		folder = Mockito.mock(Folder.class);
		canonicalFile = Mockito.mock(File.class);
		canonicalFolder = Mockito.mock(File.class);
		conflictingFile = Mockito.mock(File.class);
		conflictingFolder = Mockito.mock(File.class);
		resolved = Mockito.mock(File.class);
		unrelatedFile = Mockito.mock(File.class);

		String canonicalFileName = encode.apply("test name").get();
		String canonicalFolderName = canonicalFileName + Constants.DIR_SUFFIX;
		String conflictingFileName = canonicalFileName + " (version 2)";
		String conflictingFolderName = canonicalFolderName + " (version 2)";
		String unrelatedName = "notBa$e32!";

		Mockito.when(canonicalFile.name()).thenReturn(canonicalFileName);
		Mockito.when(canonicalFolder.name()).thenReturn(canonicalFolderName);
		Mockito.when(conflictingFile.name()).thenReturn(conflictingFileName);
		Mockito.when(conflictingFolder.name()).thenReturn(conflictingFolderName);
		Mockito.when(unrelatedFile.name()).thenReturn(unrelatedName);

		Mockito.when(canonicalFile.exists()).thenReturn(true);
		Mockito.when(canonicalFolder.exists()).thenReturn(true);
		Mockito.when(conflictingFile.exists()).thenReturn(true);
		Mockito.when(conflictingFolder.exists()).thenReturn(true);
		Mockito.when(unrelatedFile.exists()).thenReturn(true);

		Mockito.doReturn(Optional.of(folder)).when(canonicalFile).parent();
		Mockito.doReturn(Optional.of(folder)).when(canonicalFolder).parent();
		Mockito.doReturn(Optional.of(folder)).when(conflictingFile).parent();
		Mockito.doReturn(Optional.of(folder)).when(conflictingFolder).parent();
		Mockito.doReturn(Optional.of(folder)).when(unrelatedFile).parent();

		Mockito.when(folder.file(Mockito.startsWith(canonicalFileName.substring(0, 8)))).thenReturn(resolved);
		Mockito.when(folder.file(canonicalFileName)).thenReturn(canonicalFile);
		Mockito.when(folder.file(canonicalFolderName)).thenReturn(canonicalFolder);
		Mockito.when(folder.file(conflictingFileName)).thenReturn(conflictingFile);
		Mockito.when(folder.file(conflictingFolderName)).thenReturn(conflictingFolder);
		Mockito.when(folder.file(unrelatedName)).thenReturn(unrelatedFile);
	}

	@Test
	public void testCanonicalName() {
		File resolved = conflictResolver.resolveIfNecessary(canonicalFile);
		Assert.assertSame(canonicalFile, resolved);
	}

	@Test
	public void testUnrelatedName() {
		File resolved = conflictResolver.resolveIfNecessary(unrelatedFile);
		Assert.assertSame(unrelatedFile, resolved);
	}

	@Test
	public void testConflictingFile() {
		File resolved = conflictResolver.resolveIfNecessary(conflictingFile);
		Mockito.verify(conflictingFile).moveTo(resolved);
		Assert.assertSame(resolved, resolved);
	}

	@Test
	public void testConflictingFileIfCanonicalDoesnExist() {
		Mockito.when(canonicalFile.exists()).thenReturn(false);
		File resolved = conflictResolver.resolveIfNecessary(conflictingFile);
		Mockito.verify(conflictingFile).moveTo(canonicalFile);
		Assert.assertSame(canonicalFile, resolved);
	}

	@Test
	public void testConflictingFolder() {
		File resolved = conflictResolver.resolveIfNecessary(conflictingFolder);
		Mockito.verify(conflictingFolder).moveTo(resolved);
		Assert.assertSame(resolved, resolved);
	}

}
