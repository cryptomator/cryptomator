/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.crypto.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.crypto.FilenamePseudonymizing;
import de.sebastianstenzel.oce.crypto.TransactionAwareFileAccess;

public class FilenamePseudonymizerTest {
	
	private final FilenamePseudonymizing pseudonymizer = Cryptor.getDefaultCryptor();
	private Path workingDir;
	
	@Before
	public void prepareTmpDir() throws IOException {
		final String tmpDirName = (String) System.getProperties().get("java.io.tmpdir");
		final Path path = FileSystems.getDefault().getPath(tmpDirName);
		workingDir = Files.createTempDirectory(path, "oce-crypto-test");
	}
	
	@Test
	public void testCreatePseudonym() throws IOException {
		final Accessor accessor = new Accessor();
		final String originalCleartextUri = "/foo/bar/test.txt";
		
		final String pseudonym = pseudonymizer.createPseudonym(originalCleartextUri, accessor);
		Assert.assertNotNull(pseudonym);
		
		final String cleartext = pseudonymizer.uncoverPseudonym(pseudonym, accessor);
		Assert.assertEquals(originalCleartextUri, cleartext);
	}
	
	@After
	public void dropTmpDir() throws IOException {
		FileUtils.deleteDirectory(workingDir.toFile());
	}
	
	private class Accessor implements TransactionAwareFileAccess {

		@Override
		public OutputStream openFileForWrite(final Path path) throws IOException {
			Files.createDirectories(path.getParent());
			return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		}
		
		@Override
		public InputStream openFileForRead(final Path path) throws IOException {
			return Files.newInputStream(path, StandardOpenOption.READ);
		}

		@Override
		public Path resolveUri(String uri) {
			return workingDir.resolve(removeLeadingSlash(uri));
		}
		
		private String removeLeadingSlash(String path) {
			if (path.length() == 0) {
				return path;
			} else if (path.charAt(0) == '/') {
				return path.substring(1);
			} else {
				return path;
			}
		}
		
	}

}
