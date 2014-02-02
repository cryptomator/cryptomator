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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.sebastianstenzel.oce.crypto.StorageCrypting;
import de.sebastianstenzel.oce.crypto.StorageCrypting.AlreadyInitializedException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.DecryptFailedException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.InvalidStorageLocationException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.UnsupportedKeyLengthException;
import de.sebastianstenzel.oce.crypto.StorageCrypting.WrongPasswordException;
import de.sebastianstenzel.oce.crypto.aes256.AesCryptor;

public class AesCryptorTest {

	private Path workingDir;

	@Before
	public void prepareTmpDir() throws IOException {
		final String tmpDirName = (String) System.getProperties().get("java.io.tmpdir");
		final Path path = FileSystems.getDefault().getPath(tmpDirName);
		workingDir = Files.createTempDirectory(path, "oce-crypto-test");
	}

	@Test
	public void testCorrectPassword() throws IOException, AlreadyInitializedException, InvalidStorageLocationException, WrongPasswordException, DecryptFailedException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final StorageCrypting encryptor = new AesCryptor();
		encryptor.initializeStorage(workingDir, pw);
		encryptor.swipeSensitiveData();

		final StorageCrypting decryptor = new AesCryptor();
		decryptor.unlockStorage(workingDir, pw);
	}

	@Test(expected=WrongPasswordException.class)
	public void testWrongPassword() throws IOException, AlreadyInitializedException, InvalidStorageLocationException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final StorageCrypting encryptor = new AesCryptor();
		encryptor.initializeStorage(workingDir, pw);
		encryptor.swipeSensitiveData();

		final String wrongPw = "foo";
		final StorageCrypting decryptor = new AesCryptor();
		decryptor.unlockStorage(workingDir, wrongPw);
	}
	
	@Test(expected=InvalidStorageLocationException.class)
	public void testWrongLocation() throws IOException, AlreadyInitializedException, InvalidStorageLocationException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final StorageCrypting encryptor = new AesCryptor();
		encryptor.initializeStorage(workingDir, pw);
		encryptor.swipeSensitiveData();

		final Path wrongWorkginDir = workingDir.resolve("wrongSubResource");
		final StorageCrypting decryptor = new AesCryptor();
		decryptor.unlockStorage(wrongWorkginDir, pw);
	}
	
	@Test(expected=AlreadyInitializedException.class)
	public void testReInitialization() throws IOException, AlreadyInitializedException {
		final String pw = "asd";
		final StorageCrypting encryptor1 = new AesCryptor();
		encryptor1.initializeStorage(workingDir, pw);
		encryptor1.swipeSensitiveData();

		final StorageCrypting encryptor2 = new AesCryptor();
		encryptor2.initializeStorage(workingDir, pw);
		encryptor2.swipeSensitiveData();
	}

	@After
	public void dropTmpDir() throws IOException {
		FileUtils.deleteDirectory(workingDir.toFile());
	}

}
