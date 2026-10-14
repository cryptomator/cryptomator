package org.cryptomator.common.vaults;

import com.google.common.util.concurrent.MoreExecutors;
import org.cryptomator.common.Environment;
import org.cryptomator.common.mount.Mounter;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.event.FileSystemEventAggregator;
import org.cryptomator.event.NotificationManager;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class VaultTest {

	private final CryptoFileSystem cryptoFileSystem = Mockito.mock(CryptoFileSystem.class);
	private final Mounter mounter = Mockito.mock(Mounter.class);
	private final Mount mount = Mockito.mock(Mount.class);
	private final Runnable specialCleanup = Mockito.mock(Runnable.class);
	private final CompletableFuture<Void> terminated = new CompletableFuture<>();
	private final VaultState state = new VaultState(VaultState.Value.UNLOCKED);
	private MockedStatic<CryptoFileSystemProvider> cryptoFileSystemProvider;
	private MockedStatic<FileSystemCapabilityChecker> fileSystemCapabilityChecker;
	private MockedStatic<Platform> platform;
	private Vault vault;

	@BeforeEach
	public void setup() {
		cryptoFileSystemProvider = Mockito.mockStatic(CryptoFileSystemProvider.class);
		cryptoFileSystemProvider.when(() -> CryptoFileSystemProvider.newFileSystem(Mockito.any(Path.class), Mockito.any(CryptoFileSystemProperties.class))).thenReturn(cryptoFileSystem);
		fileSystemCapabilityChecker = Mockito.mockStatic(FileSystemCapabilityChecker.class);
		platform = Mockito.mockStatic(Platform.class);
		platform.when(() -> Platform.runLater(Mockito.any())).thenAnswer(invocation -> {
			invocation.getArgument(0, Runnable.class).run();
			return null;
		});
		Mockito.when(cryptoFileSystem.getRootDirectories()).thenReturn(List.of(Path.of("/")));
		Mockito.when(mount.whenTerminated()).thenReturn(terminated);
		vault = createVault(MoreExecutors.newDirectExecutorService());
	}

	@AfterEach
	public void tearDown() {
		platform.close();
		fileSystemCapabilityChecker.close();
		cryptoFileSystemProvider.close();
	}

	private Vault createVault(ExecutorService executor) {
		var vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path.set(Path.of("vault"));
		vaultSettings.usesReadOnlyMode.set(true);
		var settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		return new Vault(vaultSettings, Mockito.mock(VaultConfigCache.class), new AtomicReference<>(), state, new SimpleObjectProperty<>(), Mockito.mock(VaultStats.class), mounter, settings, Mockito.mock(FileSystemEventAggregator.class), Mockito.mock(NotificationManager.class), executor);
	}

	private void unlock(Mount mount, boolean supportsWhenTerminated) throws Exception {
		unlock(mount, false, supportsWhenTerminated);
	}

	private void unlock(Mount mount, boolean supportsUnmountForced, boolean supportsWhenTerminated) throws Exception {
		Mockito.when(mounter.mount(Mockito.any(), Mockito.any())).thenReturn(new Mounter.MountHandle(mount, supportsUnmountForced, supportsWhenTerminated, specialCleanup));
		vault.unlock(Mockito.mock(MasterkeyLoader.class));
	}

	@Test
	public void testExternalUnmountLocksVault() throws Exception {
		unlock(mount, true);

		terminated.complete(null);
		vault.lock(false);

		Assertions.assertEquals(VaultState.Value.LOCKED, state.getValue());
		Mockito.verify(mount).close();
		Mockito.verify(specialCleanup).run();
		Mockito.verify(cryptoFileSystem).close();
		Mockito.verify(mount, Mockito.never()).unmount();
	}

	@Test
	public void testExternalUnmountLocksVaultWhenCloseFails() throws Exception {
		unlock(mount, true);
		Mockito.doThrow(IOException.class).when(mount).close();

		terminated.complete(null);

		Assertions.assertEquals(VaultState.Value.LOCKED, state.getValue());
		Mockito.verify(cryptoFileSystem).close();
	}

	@Test
	public void testTerminationDuringProcessingLeavesStateToOwner() throws Exception {
		unlock(mount, true);
		state.set(VaultState.Value.PROCESSING);

		terminated.complete(null);

		Assertions.assertEquals(VaultState.Value.PROCESSING, state.getValue());
		Mockito.verify(mount).close();
		Mockito.verify(cryptoFileSystem).close();
		vault.finishProcessing();
		Assertions.assertEquals(VaultState.Value.LOCKED, state.getValue());
	}

	@Test
	public void testTerminationWhileForceDecisionPendingLocks() throws Exception {
		unlock(mount, true, true);
		state.set(VaultState.Value.PROCESSING);
		Mockito.doThrow(UnmountFailedException.class).when(mount).unmount();
		Assertions.assertThrows(UnmountFailedException.class, () -> vault.lock(false));

		terminated.complete(null);
		Assertions.assertEquals(VaultState.Value.PROCESSING, state.getValue());
		vault.lock(true);
		vault.finishProcessing();

		Mockito.verify(mount, Mockito.never()).unmountForced();
		Assertions.assertEquals(VaultState.Value.LOCKED, state.getValue());
	}

	@Test
	public void testRequestedLockIgnoresTermination() throws Exception {
		unlock(mount, true);

		vault.lock(false);
		terminated.complete(null);

		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
		Mockito.verify(mount).unmount();
		Mockito.verify(mount).close();
	}

	@Test
	public void testTerminationDuringRequestedLockWaitsForLock() throws Exception {
		var executor = Executors.newSingleThreadExecutor();
		vault = createVault(executor);
		unlock(mount, true);
		Mockito.doAnswer(_ -> {
			terminated.complete(null);
			Thread.sleep(100);
			return null;
		}).when(mount).unmount();

		vault.lock(false);
		executor.shutdown();

		Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
		Mockito.verify(mount).close();
	}

	@Test
	public void testStaleTerminationIgnoresNewerMount() throws Exception {
		var newerMount = Mockito.mock(Mount.class);
		Mockito.when(newerMount.whenTerminated()).thenReturn(new CompletableFuture<>());
		unlock(mount, true);
		vault.lock(false);
		unlock(newerMount, true);

		terminated.complete(null);

		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
		Mockito.verify(newerMount, Mockito.never()).close();
		Mockito.verify(cryptoFileSystem).close();
	}

	@Test
	public void testLockClearsMountWhenCloseFails() throws Exception {
		unlock(mount, false);
		Mockito.doThrow(IOException.class).when(mount).close();

		Assertions.assertThrows(IOException.class, () -> vault.lock(false));
		vault.lock(false);

		Mockito.verify(mount).unmount();
		Mockito.verify(specialCleanup).run();
		Mockito.verify(cryptoFileSystem).close();
	}

	@Test
	public void testLockWrapsFailedCloseAsIOException() throws Exception {
		unlock(mount, false);
		var closeFailure = new UnmountFailedException("close failed");
		Mockito.doThrow(closeFailure).when(mount).close();

		var thrown = Assertions.assertThrows(IOException.class, () -> vault.lock(false));

		Assertions.assertSame(closeFailure, thrown.getCause());
		Assertions.assertNull(vault.getMountPoint());
	}

	@Test
	public void testFinishProcessingWithMountSettlesUnlocked() throws Exception {
		unlock(mount, false);
		state.set(VaultState.Value.PROCESSING);

		vault.finishProcessing();

		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
	}

	@Test
	public void testFinishProcessingAfterFailedCloseSettlesLocked() throws Exception {
		unlock(mount, false);
		state.set(VaultState.Value.PROCESSING);
		Mockito.doThrow(IOException.class).when(mount).close();
		Assertions.assertThrows(IOException.class, () -> vault.lock(false));

		vault.finishProcessing();

		Assertions.assertEquals(VaultState.Value.LOCKED, state.getValue());
	}

	@Test
	public void testFinishProcessingWithoutOwnershipKeepsState() {
		vault.finishProcessing();

		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
	}

	@Test
	public void testMarkLockedIfUnmountedLeavesProcessing() throws Exception {
		unlock(mount, false);
		vault.lock(false);
		state.set(VaultState.Value.PROCESSING);

		Assertions.assertFalse(vault.markLockedIfUnmounted());
		Assertions.assertEquals(VaultState.Value.PROCESSING, state.getValue());
	}

	@Test
	public void testMarkLockedIfUnmountedLeavesMountedVault() throws Exception {
		unlock(mount, false);

		Assertions.assertFalse(vault.markLockedIfUnmounted());
		Assertions.assertEquals(VaultState.Value.UNLOCKED, state.getValue());
	}

	@Test
	public void testForcedUnmountUnsupportedWhenNotMounted() {
		Assertions.assertFalse(vault.supportsForcedUnmount());
	}

	@Test
	public void testUnlockWithoutCapabilityIgnoresTermination() throws Exception {
		unlock(mount, false);

		Mockito.verify(mount, Mockito.never()).whenTerminated();
	}

}
