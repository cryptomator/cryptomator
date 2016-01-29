/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.nio.ByteBuffer;

import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.impl.TestCryptorImplFactory;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MasterkeysTest {

	private FileSystem fs;
	private Masterkeys m;

	@Before
	public void setup() {
		fs = new InMemoryFileSystem();
		m = new Masterkeys(TestCryptorImplFactory::insecureCryptorImpl);
	}

	@Test
	public void testInitialize() {
		m.initialize(fs, "asd");
		Assert.assertTrue(fs.file("masterkey.cryptomator").exists());
	}

	@Test
	public void testBackup() {
		try (WritableFile w = fs.file("masterkey.cryptomator").openWritable()) {
			w.write(ByteBuffer.wrap("asd".getBytes()));
		}
		m.backup(fs);
		Assert.assertTrue(fs.file("masterkey.cryptomator.bkup").exists());
	}

	@Test
	public void testRestoreBackup() {
		try (WritableFile w = fs.file("masterkey.cryptomator.bkup").openWritable()) {
			w.write(ByteBuffer.wrap("asd".getBytes()));
		}
		m.restoreBackup(fs);
		Assert.assertTrue(fs.file("masterkey.cryptomator").exists());
	}

	@Test
	public void testChangePassphraseWithCorrectPassword() {
		m.initialize(fs, "foo");
		m.changePassphrase(fs, "foo", "bar");
		Assert.assertNotNull(m.decrypt(fs, "bar"));
	}

	@Test(expected = InvalidPassphraseException.class)
	public void testChangePassphraseWithIncorrectPassword() {
		m.initialize(fs, "foo");
		m.changePassphrase(fs, "wrong", "bar");
	}

}
