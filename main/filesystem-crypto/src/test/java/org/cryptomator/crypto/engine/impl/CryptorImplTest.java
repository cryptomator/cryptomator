/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.io.IOException;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.crypto.engine.InvalidPassphraseException;
import org.cryptomator.crypto.engine.UnsupportedVaultFormatException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CryptorImplTest {

	@Test
	public void testMasterkeyDecryptionWithCorrectPassphrase() throws IOException {
		final String testMasterKey = "{\"version\":3,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":2,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"hmacMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"versionMac\":\"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLA=\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.readKeysFromMasterkeyFile(testMasterKey.getBytes(), "asd");
	}

	@Test(expected = InvalidPassphraseException.class)
	public void testMasterkeyDecryptionWithWrongPassphrase() throws IOException {
		final String testMasterKey = "{\"version\":3,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":2,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"hmacMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"versionMac\":\"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLA=\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.readKeysFromMasterkeyFile(testMasterKey.getBytes(), "qwe");
	}

	@Test(expected = UnsupportedVaultFormatException.class)
	public void testMasterkeyDecryptionWithWrongVaultFormat() throws IOException {
		final String testMasterKey = "{\"version\":-1,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":2,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"hmacMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"versionMac\":\"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLA=\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.readKeysFromMasterkeyFile(testMasterKey.getBytes(), "asd");
	}

	@Ignore
	@Test(expected = UnsupportedVaultFormatException.class)
	public void testMasterkeyDecryptionWithMissingVersionMac() throws IOException {
		final String testMasterKey = "{\"version\":3,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":2,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"hmacMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.readKeysFromMasterkeyFile(testMasterKey.getBytes(), "asd");
	}

	@Ignore
	@Test(expected = UnsupportedVaultFormatException.class)
	public void testMasterkeyDecryptionWithWrongVersionMac() throws IOException {
		final String testMasterKey = "{\"version\":3,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":2,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"hmacMasterKey\":\"mM+qoQ+o0qvPTiDAZYt+flaC3WbpNAx1sTXaUzxwpy0M9Ctj6Tih/Q==\"," //
				+ "\"versionMac\":\"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLa=\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.readKeysFromMasterkeyFile(testMasterKey.getBytes(), "asd");
	}

	@Test
	public void testMasterkeyEncryption() throws IOException {
		final String expectedMasterKey = "{\"version\":3,\"scryptSalt\":\"AAAAAAAAAAA=\",\"scryptCostParam\":16384,\"scryptBlockSize\":8," //
				+ "\"primaryMasterKey\":\"BJPIq5pvhN24iDtPJLMFPLaVJWdGog9k4n0P03j4ru+ivbWY9OaRGQ==\"," //
				+ "\"hmacMasterKey\":\"BJPIq5pvhN24iDtPJLMFPLaVJWdGog9k4n0P03j4ru+ivbWY9OaRGQ==\"," //
				+ "\"versionMac\":\"iUmRRHITuyJsJbVNqGNw+82YQ4A3Rma7j/y1v0DCVLA=\"}";
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.randomizeMasterkey();
		final byte[] masterkeyFile = cryptor.writeKeysToMasterkeyFile("asd");
		Assert.assertArrayEquals(expectedMasterKey.getBytes(), masterkeyFile);
	}

	@Test
	public void testGetFilenameAndFileContentCryptor() throws InterruptedException {
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.randomizeMasterkey();

		Assert.assertSame(cryptor.getFilenameCryptor(), cryptor.getFilenameCryptor());
		Assert.assertSame(cryptor.getFileContentCryptor(), cryptor.getFileContentCryptor());
	}

	@Test(expected = IllegalStateException.class)
	public void testGetFilenameAndFileContentCryptorWithoutKeys() throws InterruptedException {
		final Cryptor cryptor = TestCryptorImplFactory.insecureCryptorImpl();
		cryptor.getFilenameCryptor();
	}

}
