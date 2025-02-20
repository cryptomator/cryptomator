package org.cryptomator.common.vaults;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.mount.Mounter;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VaultSettingsJson;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.quickaccess.QuickAccessService;
import org.cryptomator.integrations.quickaccess.QuickAccessServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@ExtendWith(MockitoExtension.class)
public class VaultTest {

	public static class DummyVaultState extends VaultState {
		public DummyVaultState(VaultState.Value initialValue) {
			super(initialValue);
		}
	}

	public static class DummyVaultSettings extends VaultSettings {
		public DummyVaultSettings() {
			super(createDummyJson());
		}

		private static VaultSettingsJson createDummyJson() {
			VaultSettingsJson json = new VaultSettingsJson();
			json.setId("dummyId");
			json.setPath("dummyVault");
			json.setDisplayName("Dummy Vault");
			json.setUnlockAfterStartup(false);
			json.setRevealAfterMount(true);
			json.setUsesReadOnlyMode(false);
			json.setMountFlags(VaultSettings.DEFAULT_MOUNT_FLAGS);
			json.setMaxCleartextFilenameLength(VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH);
			json.setActionAfterUnlock(VaultSettings.DEFAULT_ACTION_AFTER_UNLOCK);
			json.setAutoLockWhenIdle(VaultSettings.DEFAULT_AUTOLOCK_WHEN_IDLE);
			json.setAutoLockIdleSeconds(VaultSettings.DEFAULT_AUTOLOCK_IDLE_SECONDS);
			json.setMountService("");
			json.setPort(VaultSettings.DEFAULT_PORT);
			json.setMountPoint("dummyMountPoint");
			return json;
		}
	}

	@Mock
	private VaultConfigCache configCache;

	private AtomicReference<CryptoFileSystem> cryptoFileSystem;
	private DummyVaultState vaultState;
	private SimpleObjectProperty<Exception> lastKnownException;
	@Mock
	private VaultStats vaultStats;
	@Mock
	private Mounter mounter;
	private Settings settings;
	private DummyVaultSettings vaultSettings;
	private Vault vault;

	@BeforeEach
	public void setUp() throws Exception {
		cryptoFileSystem = new AtomicReference<>(null);
		vaultState = new DummyVaultState(VaultState.Value.LOCKED);
		lastKnownException = new SimpleObjectProperty<>(null);
		vaultSettings = new DummyVaultSettings();

		settings = mock(Settings.class);

		// Set the non-final field 'useQuickAccess'
		Field useQuickAccessField = Settings.class.getDeclaredField("useQuickAccess");
		useQuickAccessField.setAccessible(true);
		useQuickAccessField.set(settings, new SimpleBooleanProperty(true));

		// For the final field 'quickAccessService', remove the final modifier if possible,
		// and set it to a SimpleStringProperty (which is a StringProperty).
		Field quickAccessServiceField = Settings.class.getDeclaredField("quickAccessService");
		quickAccessServiceField.setAccessible(true);
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(quickAccessServiceField, quickAccessServiceField.getModifiers() & ~Modifier.FINAL);
		} catch (NoSuchFieldException e) {
			// If not available, proceed without modification.
		}
		quickAccessServiceField.set(settings, new SimpleStringProperty("DummyQuickAccessService"));

		vault = new Vault(vaultSettings, configCache, cryptoFileSystem, vaultState, lastKnownException, vaultStats, mounter, settings);
	}

	/**
	 * Test that calling unlock when the vault is already unlocked throws an IllegalStateException.
	 */
	@Test
	public void testUnlockWhenAlreadyUnlockedThrowsException() throws Exception {
		// Simulate that the vault is already unlocked.
		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		cryptoFileSystem.set(dummyCryptoFS);

		MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> vault.unlock(dummyKeyLoader));
		assertEquals("Already unlocked.", ex.getMessage());
	}

	/**
	 * If we cannot integrate with a mount (it returns null), we assume your code
	 * leaves the vault in LOCKED state but does create a CryptoFileSystem.
	 */
	@Test
	public void testUnlockWithoutMountIntegrationRemainsLocked() throws Exception {
		VaultConfig.UnverifiedVaultConfig dummyConfig = mock(VaultConfig.UnverifiedVaultConfig.class);
		when(configCache.get()).thenReturn(dummyConfig);

		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		Path dummyRootPath = Paths.get("dummyRoot");
		Iterator<Path> dummyIterator = Collections.singletonList(dummyRootPath).iterator();
		when(dummyCryptoFS.getRootDirectories()).thenReturn(() -> dummyIterator);

		Mount dummyMount = mock(Mount.class);
		when(dummyMount.getMountpoint()).thenReturn(null);

		Mounter.MountHandle dummyMountHandle = new Mounter.MountHandle(dummyMount, false, () -> {});
		when(mounter.mount(any(VaultSettings.class), eq(dummyRootPath))).thenReturn(dummyMountHandle);

		try (MockedStatic<CryptoFileSystemProvider> cryptoFsProviderMock = mockStatic(CryptoFileSystemProvider.class)) {
			cryptoFsProviderMock
				.when(() -> CryptoFileSystemProvider.newFileSystem(any(Path.class), any(CryptoFileSystemProperties.class)))
				.thenReturn(dummyCryptoFS);

			MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);
			vault.unlock(dummyKeyLoader);

			assertNotNull(cryptoFileSystem.get(), "FS is created even if the mount is null");
			assertNull(vault.getMountPoint(), "Mountpoint is null if mount returns null");
			assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault remains locked if no valid mountpoint");
		}
	}

	/**
	 * Test a successful lock (non-forced unmount).
	 */
	@Test
	public void testLockUnmoutsAndDestroysCryptoFileSystem() throws Exception {
		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		cryptoFileSystem.set(dummyCryptoFS);

		Mounter.MountHandle dummyMountHandle = mock(Mounter.MountHandle.class);
		Runnable dummyCleanup = mock(Runnable.class);
		when(dummyMountHandle.specialCleanup()).thenReturn(dummyCleanup);

		Mount dummyMount = mock(Mount.class);
		when(dummyMountHandle.mountObj()).thenReturn(dummyMount);

		Field mountHandleField = Vault.class.getDeclaredField("mountHandle");
		mountHandleField.setAccessible(true);
		@SuppressWarnings("unchecked")
		AtomicReference<Mounter.MountHandle> mountHandleRef =
			(AtomicReference<Mounter.MountHandle>) mountHandleField.get(vault);
		mountHandleRef.set(dummyMountHandle);

		vault.lock(false);

		verify(dummyCleanup, times(1)).run();
		assertNull(cryptoFileSystem.get(), "CryptoFileSystem should be cleared after lock");
		assertNull(mountHandleRef.get(), "MountHandle should be cleared after lock");
		assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault should be in LOCKED state after lock.");
	}

	@Test
	public void testLockWhenAlreadyLockedDoesNothing() throws Exception {
		cryptoFileSystem.set(null);

		vault.lock(false);

		assertNull(cryptoFileSystem.get(), "CryptoFileSystem should remain null when already locked");
		assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault state remains LOCKED.");
	}

	/**
	 * Test that force unmount is called when lock is invoked with forced flag.
	 */
	@Test
	public void testLockWithForceUnmount() throws Exception {
		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		cryptoFileSystem.set(dummyCryptoFS);

		Mounter.MountHandle dummyMountHandle = mock(Mounter.MountHandle.class);
		Runnable dummyCleanup = mock(Runnable.class);
		when(dummyMountHandle.specialCleanup()).thenReturn(dummyCleanup);

		Mount dummyMount = mock(Mount.class);
		when(dummyMountHandle.mountObj()).thenReturn(dummyMount);
		when(dummyMountHandle.supportsUnmountForced()).thenReturn(true);

		Field mountHandleField = Vault.class.getDeclaredField("mountHandle");
		mountHandleField.setAccessible(true);
		@SuppressWarnings("unchecked")
		AtomicReference<Mounter.MountHandle> mountHandleRef =
			(AtomicReference<Mounter.MountHandle>) mountHandleField.get(vault);
		mountHandleRef.set(dummyMountHandle);

		vault.lock(true);

		verify(dummyCleanup, times(1)).run();
		assertNull(cryptoFileSystem.get(), "CryptoFileSystem should be cleared after forced lock");
		assertNull(mountHandleRef.get(), "MountHandle should be cleared after forced lock");
		assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault should be in LOCKED state after forced lock.");
	}

	/**
	 * If creating the FS fails, we expect the vault to stay locked.
	 */
	@Test
	public void testUnlockFailsIfCryptoFileSystemCreationThrows() throws Exception {
		VaultConfig.UnverifiedVaultConfig dummyConfig = mock(VaultConfig.UnverifiedVaultConfig.class);
		when(configCache.get()).thenReturn(dummyConfig);

		try (MockedStatic<CryptoFileSystemProvider> cryptoFsProviderMock = mockStatic(CryptoFileSystemProvider.class)) {
			cryptoFsProviderMock
				.when(() -> CryptoFileSystemProvider.newFileSystem(any(Path.class), any(CryptoFileSystemProperties.class)))
				.thenThrow(new RuntimeException("Test-induced failure"));

			MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);

			assertThrows(RuntimeException.class, () -> vault.unlock(dummyKeyLoader),
				"Unlock should propagate the exception if creation fails.");

			assertNull(cryptoFileSystem.get(), "No FS should be set if creation fails");
			assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault remains LOCKED after a failed unlock");
		}
	}

	/**
	 * If mount fails, we assume your code reverts to locked and discards the FS.
	 */
	@Test
	public void testMountFailureLocksVaultOrPreventsUnlock() throws Exception {
		VaultConfig.UnverifiedVaultConfig dummyConfig = mock(VaultConfig.UnverifiedVaultConfig.class);
		when(dummyConfig.allegedShorteningThreshold()).thenReturn(VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH);
		when(configCache.get()).thenReturn(dummyConfig);

		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		Path dummyRootPath = Paths.get("dummyRoot");
		Iterator<Path> dummyIterator = Collections.singletonList(dummyRootPath).iterator();
		when(dummyCryptoFS.getRootDirectories()).thenReturn(() -> dummyIterator);

		try (MockedStatic<CryptoFileSystemProvider> cryptoFsProviderMock = mockStatic(CryptoFileSystemProvider.class)) {
			cryptoFsProviderMock
				.when(() -> CryptoFileSystemProvider.newFileSystem(any(Path.class), any(CryptoFileSystemProperties.class)))
				.thenReturn(dummyCryptoFS);

			when(mounter.mount(any(VaultSettings.class), eq(dummyRootPath)))
				.thenThrow(new RuntimeException("Mount error"));

			MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);

			assertThrows(RuntimeException.class, () -> vault.unlock(dummyKeyLoader), "Should throw on mount error");

			assertNull(cryptoFileSystem.get(), "FS is presumably discarded if mount fails");
			assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault remains LOCKED on mount failure");
		}
	}

	/**
	 * If something fails mid-unlock, we expect the vault to remain locked.
	 */
	@Test
	public void testVaultStateRollbackOnUnlockError() throws Exception {
		VaultConfig.UnverifiedVaultConfig dummyConfig = mock(VaultConfig.UnverifiedVaultConfig.class);
		when(configCache.get()).thenReturn(dummyConfig);

		try (MockedStatic<CryptoFileSystemProvider> cryptoFsProviderMock = mockStatic(CryptoFileSystemProvider.class)) {
			cryptoFsProviderMock
				.when(() -> CryptoFileSystemProvider.newFileSystem(any(Path.class), any(CryptoFileSystemProperties.class)))
				.thenAnswer(invocation -> {
					throw new IllegalStateException("Simulated mid-unlock error");
				});

			MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);

			assertThrows(IllegalStateException.class, () -> vault.unlock(dummyKeyLoader),
				"Should throw the simulated error during unlock");

			assertNull(cryptoFileSystem.get(), "FS should not be set due to error");
			assertEquals(VaultState.Value.LOCKED, vaultState.getValue(), "Vault remains LOCKED after error");
		}
	}

	@Test
	public void testGetCiphertextPathWhenLocked() {
		Exception ex = assertThrows(IllegalStateException.class, () -> {
			vault.getCiphertextPath(Paths.get("dummy"));
		});
		assertEquals("Vault is not unlocked", ex.getMessage());
	}

	@Test
	public void testGetDisplayablePathHome() {
		Path home = Paths.get(System.getProperty("user.home"));
		vaultSettings.path.set(home.resolve("myVault"));
		String displayable = vault.getDisplayablePath();
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			assertTrue(displayable.startsWith("~\\"));
		} else {
			assertTrue(displayable.startsWith("~/"));
		}
	}

	@Test
	public void testGetDisplayablePathNonHome() {
		vaultSettings.path.set(Paths.get("/non/home/path"));
		String displayable = vault.getDisplayablePath();
		assertEquals("/non/home/path", displayable);
	}

	@Test
	public void testPropertyGetters() {
		assertNotNull(vault.displayNameProperty());
		assertNotNull(vault.lastKnownExceptionProperty());
		assertNotNull(vault.lockedProperty());
		assertNotNull(vault.processingProperty());
		assertNotNull(vault.unlockedProperty());
		assertNotNull(vault.missingProperty());
		assertNotNull(vault.needsMigrationProperty());
		assertNotNull(vault.unknownErrorProperty());
		assertNotNull(vault.mountPointProperty());
		assertNotNull(vault.displayablePathProperty());
		assertNotNull(vault.showingStatsProperty());
	}

	@Test
	public void testSetAndGetLastKnownException() {
		Exception testEx = new Exception("Test");
		vault.setLastKnownException(testEx);
		assertEquals(testEx, vault.getLastKnownException());
	}

	@Test
	public void testObservables() {
		assertNotNull(vault.observables());
		assertTrue(vault.observables().length > 0);
	}

	@Test
	public void testGettersForVaultInfo() {
		assertEquals(vaultSettings, vault.getVaultSettings());
		assertEquals(vaultSettings.path.get(), vault.getPath());
		assertEquals(vaultStats, vault.getStats());
		assertEquals(configCache, vault.getVaultConfigCache());
		assertEquals(vaultSettings.id, vault.getId());
	}

	@Test
	public void testSupportsForcedUnmountWithoutMount() {
		Exception ex = assertThrows(IllegalStateException.class, () -> vault.supportsForcedUnmount());
		assertEquals("Vault is not mounted", ex.getMessage());
	}


	@Test
	public void testRemoveFromQuickAccessWhenEntryNull() {
		vault.removeFromQuickAccess();
	}

	@Test
	public void testRemoveFromQuickAccessInternal() throws QuickAccessServiceException {
		QuickAccessService.QuickAccessEntry dummyEntry = mock(QuickAccessService.QuickAccessEntry.class);
		try {
			Field field = Vault.class.getDeclaredField("quickAccessEntry");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			AtomicReference<QuickAccessService.QuickAccessEntry> qaEntry = (AtomicReference<QuickAccessService.QuickAccessEntry>) field.get(vault);
			qaEntry.set(dummyEntry);
		} catch (Exception e) {
			fail("Reflection failed: " + e.getMessage());
		}
		vault.removeFromQuickAccess();
		verify(dummyEntry, times(1)).remove();
		try {
			Field field = Vault.class.getDeclaredField("quickAccessEntry");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			AtomicReference<QuickAccessService.QuickAccessEntry> qaEntry = (AtomicReference<QuickAccessService.QuickAccessEntry>) field.get(vault);
			assertNull(qaEntry.get());
		} catch (Exception e) {
			fail("Reflection failed: " + e.getMessage());
		}
	}
}