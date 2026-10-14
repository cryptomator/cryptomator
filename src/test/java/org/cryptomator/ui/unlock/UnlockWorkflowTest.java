package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import java.io.IOException;

class UnlockWorkflowTest {

	private final Vault vault = Mockito.mock(Vault.class);
	private final VaultService vaultService = Mockito.mock(VaultService.class);
	private final KeyLoadingStrategy keyLoadingStrategy = Mockito.mock(KeyLoadingStrategy.class);
	private MockedStatic<Platform> platform;
	private UnlockWorkflow workflow;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		platform = Mockito.mockStatic(Platform.class);
		var vaultSettings = VaultSettings.withRandomId();
		vaultSettings.actionAfterUnlock.set(WhenUnlocked.REVEAL);
		Mockito.when(vault.getVaultSettings()).thenReturn(vaultSettings);
		workflow = new UnlockWorkflow(Mockito.mock(Stage.class), Mockito.mock(Stage.class), vault, vaultService, Mockito.mock(Lazy.class), Mockito.mock(Lazy.class), Mockito.mock(Lazy.class), Mockito.mock(FxApplicationWindows.class), keyLoadingStrategy, new SimpleObjectProperty<>(), Mockito.mock(Dialogs.class));
	}

	@AfterEach
	public void tearDown() {
		platform.close();
	}

	@Test
	public void testUnlockSettlesAfterUnlock() throws Exception {
		workflow.call();

		var inOrder = Mockito.inOrder(keyLoadingStrategy, vault);
		inOrder.verify(keyLoadingStrategy).use(Mockito.any());
		inOrder.verify(vault).finishProcessing();
	}

	@Test
	public void testFailedUnlockSettles() throws Exception {
		Mockito.doThrow(IOException.class).when(keyLoadingStrategy).use(Mockito.any());

		Assertions.assertThrows(IOException.class, workflow::call);

		Mockito.verify(vault).finishProcessing();
	}

	@Test
	public void testCancelledUnlockSettles() throws Exception {
		Mockito.doThrow(new UnlockCancelledException("cancelled")).when(keyLoadingStrategy).use(Mockito.any());

		workflow.call();

		Assertions.assertTrue(workflow.isCancelled());
		Mockito.verify(vault).finishProcessing();
	}

	@Test
	public void testSucceededRunsActionAfterUnlock() {
		Mockito.when(vault.isUnlocked()).thenReturn(true);

		workflow.succeeded();

		Mockito.verify(vaultService).reveal(vault);
	}

	@Test
	public void testSucceededSkipsActionAfterUnlockWhenVolumeEnded() {
		Mockito.when(vault.isUnlocked()).thenReturn(false);

		workflow.succeeded();

		Mockito.verify(vaultService, Mockito.never()).reveal(vault);
	}

}
