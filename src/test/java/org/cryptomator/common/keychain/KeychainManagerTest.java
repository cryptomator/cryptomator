package org.cryptomator.common.keychain;


import org.cryptomator.JavaFXUtil;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


class KeychainManagerTest {

	@BeforeAll
	public static void startup() throws InterruptedException {
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@Test
	public void testStoreAndLoad() throws KeychainAccessException {
		KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(new MapKeychainAccess()));
		keychainManager.storePassphrase("test", "Test", "asd");
		Assertions.assertArrayEquals("asd".toCharArray(), keychainManager.loadPassphrase("test"));
	}

	@Nested
	class WhenObservingProperties {

		@Test
		public void testPropertyChangesWhenStoringPassword() throws KeychainAccessException, InterruptedException {
			KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(new MapKeychainAccess()));
			ReadOnlyBooleanProperty property = keychainManager.getPassphraseStoredProperty("test");
			Assertions.assertFalse(property.get());

			keychainManager.storePassphrase("test", null, "bar");

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

	@Nested
	class WhenMigrating {

		@Test
		public void testMigrateMovesStoredEntries() throws KeychainAccessException {
			var oldProvider = new MapKeychainAccess();
			var newProvider = new MapKeychainAccess();
			oldProvider.storePassphrase("stored", "Stored", "asd");

			KeychainManager.migrate(oldProvider, newProvider, Map.of("stored", "Stored", "missing", "Missing"));

			Assertions.assertArrayEquals("asd".toCharArray(), newProvider.loadPassphrase("stored"));
			Assertions.assertNull(oldProvider.loadPassphrase("stored"));
			Assertions.assertNull(newProvider.loadPassphrase("missing"));
		}

		@Test
		public void testMigrateKeepsAllEntriesInOldProviderIfOneFails() throws KeychainAccessException {
			var oldProvider = new MapKeychainAccess();
			var newProvider = new MapKeychainAccess() {
				@Override
				public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
					if (key.equals("second")) {
						throw new KeychainAccessException("store failed");
					}
					super.storePassphrase(key, displayName, passphrase);
				}
			};
			oldProvider.storePassphrase("first", "First", "asd");
			oldProvider.storePassphrase("second", "Second", "qwe");
			var idsAndNames = new LinkedHashMap<String, String>();
			idsAndNames.put("first", "First");
			idsAndNames.put("second", "Second");

			Assertions.assertThrows(KeychainAccessException.class, () -> KeychainManager.migrate(oldProvider, newProvider, idsAndNames));

			Assertions.assertArrayEquals("asd".toCharArray(), oldProvider.loadPassphrase("first"));
			Assertions.assertArrayEquals("qwe".toCharArray(), oldProvider.loadPassphrase("second"));
			Assertions.assertNull(newProvider.loadPassphrase("first"));
		}

		@Test
		public void testMigrateKeepsEntriesIfBothProvidersShareOneStore() throws KeychainAccessException {
			var store = new HashMap<String, char[]>();
			var oldProvider = new MapKeychainAccess(store);
			var newProvider = new MapKeychainAccess(store);
			oldProvider.storePassphrase("stored", "Stored", "asd");

			KeychainManager.migrate(oldProvider, newProvider, Map.of("stored", "Stored"));

			Assertions.assertArrayEquals("asd".toCharArray(), newProvider.loadPassphrase("stored"));
		}

		@Test
		public void testMigrateRollsBackRemainingEntriesAndKeepsUnrestorableEntryIfOneRollbackFails() throws KeychainAccessException {
			var refuseStores = new AtomicBoolean(false);
			var oldProvider = new MapKeychainAccess() {
				@Override
				public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
					if (refuseStores.get() && key.equals("first")) {
						throw new KeychainAccessException("restore failed");
					}
					super.storePassphrase(key, displayName, passphrase);
				}
			};
			var newProvider = new MapKeychainAccess() {
				@Override
				public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
					if (key.equals("third")) {
						throw new KeychainAccessException("store failed");
					}
					super.storePassphrase(key, displayName, passphrase);
				}
			};
			oldProvider.storePassphrase("first", "First", "asd");
			oldProvider.storePassphrase("second", "Second", "qwe");
			oldProvider.storePassphrase("third", "Third", "yxc");
			refuseStores.set(true);
			var idsAndNames = new LinkedHashMap<String, String>();
			idsAndNames.put("first", "First");
			idsAndNames.put("second", "Second");
			idsAndNames.put("third", "Third");

			var e = Assertions.assertThrows(KeychainAccessException.class, () -> KeychainManager.migrate(oldProvider, newProvider, idsAndNames));

			Assertions.assertEquals("store failed", e.getMessage());
			Assertions.assertEquals(1, e.getSuppressed().length);
			Assertions.assertArrayEquals("asd".toCharArray(), newProvider.loadPassphrase("first"));
			Assertions.assertArrayEquals("qwe".toCharArray(), oldProvider.loadPassphrase("second"));
			Assertions.assertNull(newProvider.loadPassphrase("second"));
		}

		@Test
		public void testMigrateRestoresEntriesIfNewProviderBecomesUnavailable() throws KeychainAccessException {
			var oldProvider = new MapKeychainAccess();
			var newProvider = new MapKeychainAccess() {
				private boolean unavailable;

				@Override
				public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
					if (key.equals("second")) {
						unavailable = true;
						throw new KeychainAccessException("store failed");
					}
					super.storePassphrase(key, displayName, passphrase);
				}

				@Override
				public char[] loadPassphrase(String key) {
					return unavailable ? null : super.loadPassphrase(key);
				}

				@Override
				public void deletePassphrase(String key) throws KeychainAccessException {
					if (unavailable) {
						throw new KeychainAccessException("delete failed");
					}
					super.deletePassphrase(key);
				}
			};
			oldProvider.storePassphrase("first", "First", "asd");
			oldProvider.storePassphrase("second", "Second", "qwe");
			var idsAndNames = new LinkedHashMap<String, String>();
			idsAndNames.put("first", "First");
			idsAndNames.put("second", "Second");

			Assertions.assertThrows(KeychainAccessException.class, () -> KeychainManager.migrate(oldProvider, newProvider, idsAndNames));

			Assertions.assertArrayEquals("asd".toCharArray(), oldProvider.loadPassphrase("first"));
			Assertions.assertArrayEquals("qwe".toCharArray(), oldProvider.loadPassphrase("second"));
		}

	}

}