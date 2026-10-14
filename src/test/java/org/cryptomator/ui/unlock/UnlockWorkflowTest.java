package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.mount.FSKitExtensionSwitchedOffException;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.dialogs.SimpleDialog;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class UnlockWorkflowTest {

	private final Vault vault = Mockito.mock(Vault.class);
	private final VaultService vaultService = Mockito.mock(VaultService.class);
	private final KeyLoadingStrategy keyLoadingStrategy = Mockito.mock(KeyLoadingStrategy.class);
	private final Dialogs dialogs = Mockito.mock(Dialogs.class);
	private final FxApplicationWindows appWindows = Mockito.mock(FxApplicationWindows.class);
	private final Stage mainWindow = Mockito.mock(Stage.class);
	private final Stage window = Mockito.mock(Stage.class);
	private final Stage owner = Mockito.mock(Stage.class);
	private final SimpleDialog.Builder dialogBuilder = Mockito.mock(SimpleDialog.Builder.class);
	private final SimpleDialog dialog = Mockito.mock(SimpleDialog.class);
	private final VaultSettings vaultSettings = VaultSettings.withRandomId();
	private MockedStatic<Platform> platform;
	private UnlockWorkflow workflow;

	@BeforeEach
	public void setup() {
		platform = Mockito.mockStatic(Platform.class);
		vaultSettings.actionAfterUnlock.set(WhenUnlocked.REVEAL);
		Mockito.when(vault.getVaultSettings()).thenReturn(vaultSettings);
		Mockito.when(dialogBuilder.build()).thenReturn(dialog);
		workflow = createWorkflow(owner);
	}

	@SuppressWarnings("unchecked")
	private UnlockWorkflow createWorkflow(Stage unlockOwner) {
		return new UnlockWorkflow(mainWindow, window, unlockOwner, vault, vaultService, Mockito.mock(Lazy.class), Mockito.mock(Lazy.class), Mockito.mock(Lazy.class), appWindows, keyLoadingStrategy, new SimpleObjectProperty<>(), dialogs);
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

	@Test
	public void testExtensionSwitchedOffShowsDialog() throws Exception {
		Mockito.when(dialogs.prepareFSKitExtensionSwitchedOffDialog(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dialogBuilder);

		runFailingWith(new FSKitExtensionSwitchedOffException(new MountFailedException("switched off")));

		Mockito.verify(window).close();
		Mockito.verify(dialog).showAndWait();
		Mockito.verify(appWindows, Mockito.never()).showErrorWindow(Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void testExtensionSwitchedOffRetryRestartsUnlockWithOwner() throws Exception {
		assertExtensionSwitchedOffRetryRestartsUnlockWith(owner);
	}

	@Test
	public void testExtensionSwitchedOffRetryRestartsUnlockWithoutOwner() throws Exception {
		assertExtensionSwitchedOffRetryRestartsUnlockWith(null);
	}

	private void assertExtensionSwitchedOffRetryRestartsUnlockWith(Stage unlockOwner) throws Exception {
		workflow = createWorkflow(unlockOwner);
		Mockito.when(dialogs.prepareFSKitExtensionSwitchedOffDialog(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dialogBuilder);
		runFailingWith(new FSKitExtensionSwitchedOffException(new MountFailedException("switched off")));
		ArgumentCaptor<Consumer<Stage>> okAction = ArgumentCaptor.captor();
		Mockito.verify(dialogs).prepareFSKitExtensionSwitchedOffDialog(Mockito.any(), Mockito.any(), okAction.capture());
		var dialogStage = Mockito.mock(Stage.class);

		okAction.getValue().accept(dialogStage);

		Mockito.verify(dialogStage).close();
		Mockito.verify(appWindows).startUnlockWorkflow(vault, unlockOwner);
		Assertions.assertFalse(vaultSettings.usesReadOnlyMode.get());
	}

	@Test
	public void testReadOnlyRetryRestartsUnlockWithMainWindow() throws Exception {
		Mockito.when(dialogs.prepareRetryIfReadonlyDialog(Mockito.any(), Mockito.any())).thenReturn(dialogBuilder);
		runFailingWith(new ReadOnlyFileSystemException());
		ArgumentCaptor<Consumer<Stage>> okAction = ArgumentCaptor.captor();
		Mockito.verify(dialogs).prepareRetryIfReadonlyDialog(Mockito.any(), okAction.capture());

		okAction.getValue().accept(Mockito.mock(Stage.class));

		Assertions.assertTrue(vaultSettings.usesReadOnlyMode.get());
		Mockito.verify(appWindows).startUnlockWorkflow(vault, mainWindow);
	}

	private void runFailingWith(Exception exception) throws Exception {
		platform.when(Platform::isFxApplicationThread).thenReturn(true);
		platform.when(() -> Platform.runLater(Mockito.any())).thenAnswer(invocation -> {
			invocation.getArgument(0, Runnable.class).run();
			return null;
		});
		var vaultState = Mockito.mock(VaultState.class);
		Mockito.when(vaultState.awaitState(VaultState.Value.LOCKED, 5, TimeUnit.SECONDS)).thenReturn(true);
		Mockito.when(vault.stateProperty()).thenReturn(vaultState);
		Mockito.doThrow(exception).when(keyLoadingStrategy).use(Mockito.any());

		workflow.run();
	}

}
