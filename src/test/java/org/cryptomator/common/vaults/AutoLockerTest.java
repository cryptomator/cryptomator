package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VaultSettingsJson;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class AutoLockerTest {

	private AutoLocker autoLocker;
	private ScheduledExecutorService mockScheduler;
	private ObservableList<Vault> vaultList;
	private Supplier<Instant> mockTimeProvider;
	private Vault mockVault;

	@BeforeEach
	public void setUp() {
		mockScheduler = mock(ScheduledExecutorService.class);
		vaultList = FXCollections.observableArrayList();
		mockTimeProvider = mock(Supplier.class);
		autoLocker = new AutoLocker(mockScheduler, vaultList, mockTimeProvider);

		// Mock Vault
		mockVault = mock(Vault.class);

		// Use a real VaultSettings instance instead of mocking
		VaultSettings vaultSettings = new VaultSettings(new VaultSettingsJson());
		vaultSettings.autoLockWhenIdle.set(true);
		vaultSettings.autoLockIdleSeconds.set(300);

		when(mockVault.getVaultSettings()).thenReturn(vaultSettings);
		when(mockVault.isUnlocked()).thenReturn(true);

		// Create and stub the VaultStats mock separately
		VaultStats mockStats = mock(VaultStats.class);
		when(mockVault.getStats()).thenReturn(mockStats);
		when(mockStats.getLastActivity()).thenReturn(Instant.now().minusSeconds(600));
	}

	@Test
	public void testDoesNotAutolockWhenIdleTimeNotExceeded() throws IOException, UnmountFailedException {
		Instant lastActivity = Instant.now().minusSeconds(100); // Last activity 100 seconds ago
		when(mockVault.getStats().getLastActivity()).thenReturn(lastActivity);
		when(mockTimeProvider.get()).thenReturn(Instant.now());

		vaultList.add(mockVault);
		autoLocker.tick();

		verify(mockVault, never()).lock(false);
	}

	@Test
	public void testInitSchedulesTask() {
		autoLocker.init();
		verify(mockScheduler, times(1)).scheduleAtFixedRate(any(), eq(0L), eq(1L), eq(TimeUnit.MINUTES));
	}
}
