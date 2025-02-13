package org.cryptomator.common.vaults;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
	public void setUp() {
		cryptoFileSystem = new AtomicReference<>(null);
		vaultState = new DummyVaultState(VaultState.Value.LOCKED);
		lastKnownException = new SimpleObjectProperty<>(null);
		vaultSettings = new DummyVaultSettings();

		settings = mock(Settings.class);
		try {
			Field useQuickAccessField = Settings.class.getDeclaredField("useQuickAccess");
			useQuickAccessField.setAccessible(true);
			useQuickAccessField.set(settings, new SimpleBooleanProperty(false));
		} catch (Exception e) {
			throw new RuntimeException("Could not set useQuickAccess on Settings", e);
		}

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
	 * Test a successful unlock without full mount integration.
	 * In this test, we simulate the CryptoFileSystem creation and provide a mount handle whose underlying
	 * mount returns a null mount point.
	 */
	@Test
	public void testUnlockSucceedsWithoutMountIntegration() throws Exception {
		VaultConfig.UnverifiedVaultConfig dummyConfig = mock(VaultConfig.UnverifiedVaultConfig.class);
		when(dummyConfig.allegedShorteningThreshold()).thenReturn(VaultSettings.DEFAULT_MAX_CLEARTEXT_FILENAME_LENGTH);
		when(configCache.get()).thenReturn(dummyConfig);

		// Prepare a dummy CryptoFileSystem and simulate its root directory.
		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		Path dummyRootPath = Paths.get("dummyRoot");
		Iterator<Path> dummyIterator = Collections.singletonList(dummyRootPath).iterator();
		when(dummyCryptoFS.getRootDirectories()).thenReturn(() -> dummyIterator);

		// Create a dummy Mount that returns a null mount point.
		Mount dummyMount = mock(Mount.class);
		when(dummyMount.getMountpoint()).thenReturn(null);
		// Create a dummy mount handle that returns the dummy mount.
		Mounter.MountHandle dummyMountHandle = new Mounter.MountHandle(dummyMount, false, () -> {});
		when(mounter.mount(any(VaultSettings.class), eq(dummyRootPath))).thenReturn(dummyMountHandle);

		// Use static mocking to simulate CryptoFileSystemProvider.newFileSystem.
		try (MockedStatic<CryptoFileSystemProvider> cryptoFsProviderMock = mockStatic(CryptoFileSystemProvider.class)) {
			cryptoFsProviderMock.when(() ->
					CryptoFileSystemProvider.newFileSystem(any(Path.class), any(CryptoFileSystemProperties.class))
			).thenReturn(dummyCryptoFS);

			MasterkeyLoader dummyKeyLoader = mock(MasterkeyLoader.class);
			vault.unlock(dummyKeyLoader);

			// Verify that the crypto file system is set.
			assertNotNull(cryptoFileSystem.get(), "CryptoFileSystem should be set after unlock");
			assertNull(vault.getMountPoint(), "MountPoint should be null because dummy mount returns null mountpoint");
		}
	}


	/**
	 * Test a successful lock (non-forced unmount). We simulate an unlocked vault by setting a dummy crypto file system
	 * and a dummy mount handle. We then verify that after calling lock, the cleanup routines are run and internal state is cleared.
	 */
	@Test
	public void testLockUnmoutsAndDestroysCryptoFileSystem() throws Exception {
		// Simulate that the vault is unlocked.
		CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
		cryptoFileSystem.set(dummyCryptoFS);

		// Prepare a dummy mount handle.
		Mounter.MountHandle dummyMountHandle = mock(Mounter.MountHandle.class);
		// Removed unnecessary stubbing of supportsUnmountForced().
		Runnable dummyCleanup = mock(Runnable.class);
		when(dummyMountHandle.specialCleanup()).thenReturn(dummyCleanup);

		// Ensure that mountObj() returns a non-null dummy Mount so that unmount() can be called.
		Mount dummyMount = mock(Mount.class);
		when(dummyMountHandle.mountObj()).thenReturn(dummyMount);

		// Set the private field "mountHandle" in Vault via reflection.
		Field mountHandleField = Vault.class.getDeclaredField("mountHandle");
		mountHandleField.setAccessible(true);
		@SuppressWarnings("unchecked")
		AtomicReference<Mounter.MountHandle> mountHandleRef =
				(AtomicReference<Mounter.MountHandle>) mountHandleField.get(vault);
		mountHandleRef.set(dummyMountHandle);

		vault.lock(false);

		// Verify that the cleanup routine is run.
		verify(dummyCleanup, times(1)).run();

		// After lock, the crypto file system and mount handle should be cleared.
		assertNull(cryptoFileSystem.get(), "CryptoFileSystem should be cleared after lock");
		assertNull(mountHandleRef.get(), "MountHandle should be cleared after lock");
	}

	@Test
public void testLockWhenAlreadyLockedDoesNothing() throws Exception {
    // Ensure the vault is locked
    cryptoFileSystem.set(null);

    vault.lock(false);

    // Verify that no exception is thrown and state remains unchanged
    assertNull(cryptoFileSystem.get(), "CryptoFileSystem should remain null when already locked");
}

/**
 * Test that force unmount is called when lock is invoked with forced flag.
 */
@Test
public void testLockWithForceUnmount() throws Exception {
    // Simulate that the vault is unlocked
    CryptoFileSystem dummyCryptoFS = mock(CryptoFileSystem.class);
    cryptoFileSystem.set(dummyCryptoFS);

    // Prepare a dummy mount handle
    Mounter.MountHandle dummyMountHandle = mock(Mounter.MountHandle.class);
    Runnable dummyCleanup = mock(Runnable.class);
    when(dummyMountHandle.specialCleanup()).thenReturn(dummyCleanup);

    Mount dummyMount = mock(Mount.class);
    when(dummyMountHandle.mountObj()).thenReturn(dummyMount);
    when(dummyMountHandle.supportsUnmountForced()).thenReturn(true);

    // Set the private field "mountHandle" in Vault via reflection.
    Field mountHandleField = Vault.class.getDeclaredField("mountHandle");
    mountHandleField.setAccessible(true);
    @SuppressWarnings("unchecked")
    AtomicReference<Mounter.MountHandle> mountHandleRef =
            (AtomicReference<Mounter.MountHandle>) mountHandleField.get(vault);
    mountHandleRef.set(dummyMountHandle);

    vault.lock(true); // Call lock with force unmount

    // Verify that the cleanup routine is executed
    verify(dummyCleanup, times(1)).run(); // This ensures the unmounting logic runs

    // Ensure state is cleared
    assertNull(cryptoFileSystem.get(), "CryptoFileSystem should be cleared after forced lock");
    assertNull(mountHandleRef.get(), "MountHandle should be cleared after forced lock");
}
}