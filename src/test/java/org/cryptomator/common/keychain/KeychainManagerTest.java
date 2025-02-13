package org.cryptomator.common.keychain;

import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeychainManagerTest {

	private static KeychainManager keychainManager;

	@BeforeAll
	public static void setup() {
		keychainManager = new KeychainManager(new SimpleObjectProperty<>(new MapKeychainAccess()));
	}

	@Test
	public void testStoreAndLoadValidPassphrase() throws KeychainAccessException {
		keychainManager.storePassphrase("testKey", "Test Display", "securePass", false);
		Assertions.assertArrayEquals("securePass".toCharArray(), keychainManager.loadPassphrase("testKey"));
	}

	@Test
	public void testStoreNullPassphrase() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			keychainManager.storePassphrase("nullKey", "Test Display", null, false);
		});
	}


	@Test
	public void testChangePassphrase() throws KeychainAccessException {
		keychainManager.storePassphrase("changeKey", "Test Display", "initialPass", false);
		keychainManager.changePassphrase("changeKey", "Test Display", "newPass");
		Assertions.assertArrayEquals("newPass".toCharArray(), keychainManager.loadPassphrase("changeKey"));
	}


	@Nested
	public static class ObservablePropertiesTest {

		@BeforeAll
		public static void startup() throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Platform.startup(latch::countDown);
			var javafxStarted = latch.await(5, TimeUnit.SECONDS);
			Assertions.assertTrue(javafxStarted);
		}

		@Test
		public void testPropertyChangesWhenStoringPassword() throws KeychainAccessException, InterruptedException {
			ReadOnlyBooleanProperty property = keychainManager.getPassphraseStoredProperty("propKey");
			Assertions.assertFalse(property.get());

			keychainManager.storePassphrase("propKey", "Test Display", "password", false);

			AtomicBoolean result = new AtomicBoolean(false);
			CountDownLatch latch = new CountDownLatch(1);
			Platform.runLater(() -> {
				result.set(property.get());
				latch.countDown();
			});
			Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> latch.await());
			Assertions.assertTrue(result.get());
		}
	}
}
