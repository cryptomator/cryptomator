package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.VaultSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoLockerTest {

	private ScheduledExecutorService scheduler;
	private ObservableList<Vault> vaultList;
	private MockedStatic<Platform> platform;
	private AutoLocker autoLocker;

	@BeforeEach
	void setup() {
		scheduler = mock(ScheduledExecutorService.class);
		doAnswer(invocation -> {
			invocation.getArgument(0, Runnable.class).run(); // run synchronously
			return null;
		}).when(scheduler).execute(any());
		vaultList = FXCollections.observableArrayList();
		platform = Mockito.mockStatic(Platform.class); // no FX toolkit in unit tests
		autoLocker = new AutoLocker(scheduler, vaultList);
	}

	@AfterEach
	void teardown() {
		platform.close();
	}

	private Vault vault(boolean unlocked, boolean lockOnSystemLock) {
		var settings = VaultSettings.withRandomId();
		settings.autoLockOnSystemLock.set(lockOnSystemLock);
		var vault = mock(Vault.class);
		when(vault.isUnlocked()).thenReturn(unlocked);
		when(vault.getVaultSettings()).thenReturn(settings);
		when(vault.getDisplayName()).thenReturn(settings.id);
		vaultList.add(vault);
		return vault;
	}

	@Test
	void testLockOnSystemLockOnlyLocksUnlockedOptedInVaults() throws Exception {
		var optedIn = vault(true, true);
		var optedOut = vault(true, false);
		var alreadyLocked = vault(false, true);

		autoLocker.lockOnSystemLock("test");

		verify(optedIn).lock(false);
		verify(optedOut, never()).lock(Mockito.anyBoolean());
		verify(alreadyLocked, never()).lock(Mockito.anyBoolean());
	}

	@Test
	void testLockOnSystemLockContinuesAfterFailure() throws Exception {
		var failing = vault(true, true);
		var other = vault(true, true);
		doThrow(new IOException("file in use")).when(failing).lock(false);

		autoLocker.lockOnSystemLock("test");

		verify(failing).lock(false);
		verify(other).lock(false);
	}

	@Test
	void testLockOnSystemLockIsDisabledByDefault() {
		var settings = VaultSettings.withRandomId();

		Assertions.assertFalse(settings.autoLockOnSystemLock.get());
	}

}
