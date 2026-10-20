package org.cryptomator.common.settings;

import org.cryptomator.common.Environment;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class DeviceKeyTest {

	@TempDir
	Path tmpDir;

	private Path p12File;
	private KeychainManager keychainManager;
	private DeviceKey deviceKey;

	@BeforeEach
	public void setup() {
		p12File = tmpDir.resolve("key.p12");
		var env = Mockito.mock(Environment.class);
		Mockito.when(env.getP12Path()).thenAnswer(_ -> Stream.of(p12File));
		keychainManager = Mockito.mock(KeychainManager.class);
		Mockito.when(keychainManager.isSupported()).thenReturn(true);
		deviceKey = new DeviceKey(keychainManager, env, new SecureRandom());
	}

	@Test
	public void testStoresPassphraseAfterWritingNewKeyPair() throws Exception {
		var storedPassphrase = new AtomicReference<char[]>();
		Mockito.doAnswer(invocation -> {
			CharSequence passphrase = invocation.getArgument(2);
			storedPassphrase.set(passphrase.toString().toCharArray());
			Assertions.assertDoesNotThrow(() -> P384KeyPair.load(tmpDir.resolve("key.p12.tmp"), storedPassphrase.get()));
			return null;
		}).when(keychainManager).storePassphrase(ArgumentMatchers.eq(DeviceKey.KEYCHAIN_KEY), ArgumentMatchers.any(), ArgumentMatchers.any());

		var keyPair = deviceKey.get();

		var storedKeyPair = P384KeyPair.load(p12File, storedPassphrase.get());
		Assertions.assertArrayEquals(keyPair.getPublic().getEncoded(), storedKeyPair.getPublic().getEncoded());
	}

	@Test
	public void testRegeneratesKeyPairIfPassphraseNotFound() throws Exception {
		var oldKeyPair = P384KeyPair.generate();
		oldKeyPair.store(p12File, "asd".toCharArray());
		Mockito.when(keychainManager.loadPassphrase(DeviceKey.KEYCHAIN_KEY)).thenReturn(null);

		var keyPair = deviceKey.get();

		Assertions.assertFalse(Arrays.equals(oldKeyPair.getPublic().getEncoded(), keyPair.getPublic().getEncoded()));
	}

	@Test
	public void testKeepsKeyPairIfKeychainFails() throws Exception {
		P384KeyPair.generate().store(p12File, "asd".toCharArray());
		var p12Content = Files.readAllBytes(p12File);
		Mockito.when(keychainManager.loadPassphrase(DeviceKey.KEYCHAIN_KEY)).thenThrow(new KeychainAccessException("access failed"));

		Assertions.assertThrows(DeviceKey.DeviceKeyRetrievalException.class, deviceKey::get);
		Assertions.assertArrayEquals(p12Content, Files.readAllBytes(p12File));
		Mockito.verify(keychainManager, Mockito.never()).storePassphrase(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	@Test
	public void testRegeneratesKeyPairIfPassphraseDoesNotMatch() throws Exception {
		var oldKeyPair = P384KeyPair.generate();
		oldKeyPair.store(p12File, "asd".toCharArray());
		Mockito.when(keychainManager.loadPassphrase(DeviceKey.KEYCHAIN_KEY)).thenReturn("qwe".toCharArray());

		var keyPair = deviceKey.get();

		Assertions.assertFalse(Arrays.equals(oldKeyPair.getPublic().getEncoded(), keyPair.getPublic().getEncoded()));
		Mockito.verify(keychainManager).storePassphrase(ArgumentMatchers.eq(DeviceKey.KEYCHAIN_KEY), ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	@Test
	public void testKeepsExistingKeyPairIfStoringPassphraseFails() throws Exception {
		P384KeyPair.generate().store(p12File, "asd".toCharArray());
		var p12Content = Files.readAllBytes(p12File);
		Mockito.when(keychainManager.loadPassphrase(DeviceKey.KEYCHAIN_KEY)).thenReturn(null);
		Mockito.doThrow(new KeychainAccessException("store failed")).when(keychainManager).storePassphrase(ArgumentMatchers.eq(DeviceKey.KEYCHAIN_KEY), ArgumentMatchers.any(), ArgumentMatchers.any());

		Assertions.assertThrows(DeviceKey.DeviceKeyRetrievalException.class, deviceKey::get);
		Assertions.assertArrayEquals(p12Content, Files.readAllBytes(p12File));
		Assertions.assertEquals(List.of(p12File), listTmpDir());
	}

	@Test
	public void testReplacesLeftoverFromPreviousAttempt() throws Exception {
		Files.writeString(tmpDir.resolve("key.p12.tmp"), "leftover");
		var storedPassphrase = new AtomicReference<char[]>();
		Mockito.doAnswer(invocation -> {
			CharSequence passphrase = invocation.getArgument(2);
			storedPassphrase.set(passphrase.toString().toCharArray());
			return null;
		}).when(keychainManager).storePassphrase(ArgumentMatchers.eq(DeviceKey.KEYCHAIN_KEY), ArgumentMatchers.any(), ArgumentMatchers.any());

		var keyPair = deviceKey.get();

		var storedKeyPair = P384KeyPair.load(p12File, storedPassphrase.get());
		Assertions.assertArrayEquals(keyPair.getPublic().getEncoded(), storedKeyPair.getPublic().getEncoded());
		Assertions.assertEquals(List.of(p12File), listTmpDir());
	}

	@Test
	public void testKeepsKeyPairIfFileIsUnreadable() throws Exception {
		Files.writeString(p12File, "not a PKCS#12 file");
		var p12Content = Files.readAllBytes(p12File);
		Mockito.when(keychainManager.loadPassphrase(DeviceKey.KEYCHAIN_KEY)).thenReturn("asd".toCharArray());

		Assertions.assertThrows(DeviceKey.DeviceKeyRetrievalException.class, deviceKey::get);
		Assertions.assertArrayEquals(p12Content, Files.readAllBytes(p12File));
		Mockito.verify(keychainManager, Mockito.never()).storePassphrase(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	private List<Path> listTmpDir() throws IOException {
		try (var files = Files.list(tmpDir)) {
			return files.toList();
		}
	}

}
