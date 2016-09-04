package org.cryptomator.keychain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.jni.WinDataProtection;
import org.cryptomator.jni.WinFunctions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class WindowsProtectedKeychainAccessTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Path tmpFile;
	private WindowsProtectedKeychainAccess keychain;

	@Before
	public void setup() throws IOException, ReflectiveOperationException {
		tmpFile = Files.createTempFile("unit-tests", ".tmp");
		System.setProperty("cryptomator.keychainPath", tmpFile.toAbsolutePath().normalize().toString());
		WinFunctions winFunctions = Mockito.mock(WinFunctions.class);
		WinDataProtection winDataProtection = Mockito.mock(WinDataProtection.class);
		Mockito.when(winFunctions.dataProtection()).thenReturn(winDataProtection);
		Answer<byte[]> answerReturningFirstArg = invocation -> invocation.getArgumentAt(0, byte[].class).clone();
		Mockito.when(winDataProtection.protect(Mockito.any(), Mockito.any())).thenAnswer(answerReturningFirstArg);
		Mockito.when(winDataProtection.unprotect(Mockito.any(), Mockito.any())).thenAnswer(answerReturningFirstArg);
		keychain = new WindowsProtectedKeychainAccess(Optional.of(winFunctions));
	}

	@After
	public void teardown() throws IOException {
		Files.deleteIfExists(tmpFile);
	}

	@Test
	public void testStoreAndLoad() {
		String storedPw1 = "topSecret";
		String storedPw2 = "bottomSecret";
		keychain.storePassphrase("myPassword", storedPw1);
		keychain.storePassphrase("myOtherPassword", storedPw2);
		String loadedPw1 = new String(keychain.loadPassphrase("myPassword"));
		String loadedPw2 = new String(keychain.loadPassphrase("myOtherPassword"));
		Assert.assertEquals(storedPw1, loadedPw1);
		Assert.assertEquals(storedPw2, loadedPw2);
		keychain.deletePassphrase("myPassword");
		Assert.assertNull(keychain.loadPassphrase("myPassword"));
		Assert.assertNull(keychain.loadPassphrase("nonExistingPassword"));
	}

}
