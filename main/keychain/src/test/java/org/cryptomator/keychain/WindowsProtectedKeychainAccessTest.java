/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import org.cryptomator.common.Environment;
import org.cryptomator.jni.WinDataProtection;
import org.cryptomator.jni.WinFunctions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class WindowsProtectedKeychainAccessTest {

	private WindowsProtectedKeychainAccess keychain;

	@BeforeEach
	public void setup(@TempDir Path tempDir) {
		Path keychainPath = tempDir.resolve("keychainfile.tmp");
		Environment env = Mockito.mock(Environment.class);
		Mockito.when(env.getKeychainPath()).thenReturn(Stream.of(keychainPath));
		WinFunctions winFunctions = Mockito.mock(WinFunctions.class);
		WinDataProtection winDataProtection = Mockito.mock(WinDataProtection.class);
		Mockito.when(winFunctions.dataProtection()).thenReturn(winDataProtection);
		Answer<byte[]> answerReturningFirstArg = invocation -> ((byte[]) invocation.getArgument(0)).clone();
		Mockito.when(winDataProtection.protect(Mockito.any(), Mockito.any())).thenAnswer(answerReturningFirstArg);
		Mockito.when(winDataProtection.unprotect(Mockito.any(), Mockito.any())).thenAnswer(answerReturningFirstArg);
		keychain = new WindowsProtectedKeychainAccess(Optional.of(winFunctions), env);
	}

	@Test
	public void testStoreAndLoad() {
		String storedPw1 = "topSecret";
		String storedPw2 = "bottomSecret";
		keychain.storePassphrase("myPassword", storedPw1);
		keychain.storePassphrase("myOtherPassword", storedPw2);
		String loadedPw1 = new String(keychain.loadPassphrase("myPassword"));
		String loadedPw2 = new String(keychain.loadPassphrase("myOtherPassword"));
		Assertions.assertEquals(storedPw1, loadedPw1);
		Assertions.assertEquals(storedPw2, loadedPw2);
		keychain.deletePassphrase("myPassword");
		Assertions.assertNull(keychain.loadPassphrase("myPassword"));
		Assertions.assertNotNull(keychain.loadPassphrase("myOtherPassword"));
		Assertions.assertNull(keychain.loadPassphrase("nonExistingPassword"));
	}

}
