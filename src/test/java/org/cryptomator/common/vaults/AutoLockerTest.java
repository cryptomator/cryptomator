package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.VaultSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javafx.collections.FXCollections;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class AutoLockerTest {

	private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
	private final Vault vault = Mockito.mock(Vault.class);
	private Runnable tick;

	@BeforeEach
	public void setup() {
		var vaultSettings = VaultSettings.withRandomId();
		vaultSettings.autoLockWhenIdle.set(true);
		vaultSettings.autoLockIdleSeconds.set(0);
		var stats = Mockito.mock(VaultStats.class);
		Mockito.when(stats.getLastActivity()).thenReturn(Instant.now().minusSeconds(60));
		Mockito.when(vault.isUnlocked()).thenReturn(true);
		Mockito.when(vault.getVaultSettings()).thenReturn(vaultSettings);
		Mockito.when(vault.getStats()).thenReturn(stats);

		new AutoLocker(scheduler, FXCollections.observableArrayList(vault)).init();
		var tickCaptor = ArgumentCaptor.forClass(Runnable.class);
		Mockito.verify(scheduler).scheduleAtFixedRate(tickCaptor.capture(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
		tick = tickCaptor.getValue();
	}

	@Test
	public void testAutolockMarksLockedAfterLock() throws Exception {
		tick.run();

		var inOrder = Mockito.inOrder(vault);
		inOrder.verify(vault).lock(false);
		inOrder.verify(vault).markLockedIfUnmounted();
		Mockito.verify(vault, Mockito.never()).stateProperty();
	}

	@Test
	public void testAutolockMarksLockedWhenLockFails() throws Exception {
		Mockito.doThrow(IOException.class).when(vault).lock(false);

		tick.run();

		Mockito.verify(vault).markLockedIfUnmounted();
	}

}
