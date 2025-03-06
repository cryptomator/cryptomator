package org.cryptomator.common.vaults;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VaultSettingsJson;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AutoLockerTest {

	private AutoLocker autoLocker;
	private ScheduledExecutorService mockScheduler;
	private ObservableList<Vault> vaultList;
	private Supplier<Instant> mockTimeProvider;
	private Vault mockVault;
	private VaultStats mockStats;
	private VaultSettings vaultSettings;

	@BeforeEach
	public void setUp() {
		mockScheduler = mock(ScheduledExecutorService.class);
		vaultList = FXCollections.observableArrayList();
		mockTimeProvider = mock(Supplier.class);
		autoLocker = new AutoLocker(mockScheduler, vaultList, mockTimeProvider);

		// Mock Vault
		mockVault = mock(Vault.class);

		// Use a real VaultSettings instance instead of mocking
		vaultSettings = new VaultSettings(new VaultSettingsJson());
		vaultSettings.autoLockWhenIdle.set(true);
		vaultSettings.autoLockIdleSeconds.set(300);

		when(mockVault.getVaultSettings()).thenReturn(vaultSettings);
		when(mockVault.isUnlocked()).thenReturn(true);

		// Create and stub the VaultStats mock separately
		mockStats = mock(VaultStats.class);
		when(mockVault.getStats()).thenReturn(mockStats);
		when(mockStats.getLastActivity()).thenReturn(Instant.now().minusSeconds(600));
	}

	@Test
	public void testDoesNotAutolockWhenIdleTimeNotExceeded() throws IOException, UnmountFailedException {
		Instant lastActivity = Instant.now().minusSeconds(100); // Last activity 100 seconds ago
		when(mockStats.getLastActivity()).thenReturn(lastActivity);
		when(mockTimeProvider.get()).thenReturn(Instant.now());

		vaultList.add(mockVault);
		autoLocker.tick();

		verify(mockVault, never()).lock(false);
	}


	@Test
	public void testDoesNotLockWhenAutoLockDisabled() throws IOException, UnmountFailedException {
		// Disable auto-lock
		vaultSettings.autoLockWhenIdle.set(false);

		when(mockStats.getLastActivity()).thenReturn(Instant.now().minusSeconds(600));
		when(mockTimeProvider.get()).thenReturn(Instant.now());

		vaultList.add(mockVault);
		autoLocker.tick();

		verify(mockVault, never()).lock(false);
	}

	@Test
	public void testDoesNotLockIfVaultAlreadyLocked() throws IOException, UnmountFailedException {
		// Set vault as already locked
		when(mockVault.isUnlocked()).thenReturn(false);

		vaultList.add(mockVault);
		autoLocker.tick();

		verify(mockVault, never()).lock(false);
	}




	@Test
	public void testHandlesLockExceptionGracefully() throws IOException, UnmountFailedException {
		when(mockStats.getLastActivity()).thenReturn(Instant.now().minusSeconds(1000));
		when(mockTimeProvider.get()).thenReturn(Instant.now());

		// Simulate exception when trying to lock the vault
		doThrow(new IOException("Lock failed")).when(mockVault).lock(false);

		vaultList.add(mockVault);
		autoLocker.tick();

		// Ensure AutoLocker does not crash and still attempts to lock the vault
		verify(mockVault, times(1)).lock(false);
	}
	
	@Test
	public void testInitSchedulesTask() {
		autoLocker.init();
		verify(mockScheduler, times(1)).scheduleAtFixedRate(any(), eq(0L), eq(1L), eq(TimeUnit.MINUTES));
	}
}
