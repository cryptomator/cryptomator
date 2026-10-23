package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.error.ErrorComponent;
import org.cryptomator.ui.eventview.EventViewComponent;
import org.cryptomator.ui.lock.LockComponent;
import org.cryptomator.ui.lock.LockWorkflow;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.notification.NotificationComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.sharevault.ShareVaultComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.cryptomator.ui.unlock.UnlockWorkflow;
import org.cryptomator.ui.updatereminder.UpdateReminderComponent;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

class FxApplicationWindowsTest {

	private final Stage primaryStage = Mockito.mock(Stage.class);
	private final Stage owner = Mockito.mock(Stage.class);
	private final Vault vault = Mockito.mock(Vault.class);
	private final UnlockComponent.Factory unlockWorkflowFactory = Mockito.mock(UnlockComponent.Factory.class);
	private final UnlockWorkflow unlockWorkflow = Mockito.mock(UnlockWorkflow.class);
	private final LockComponent.Factory lockWorkflowFactory = Mockito.mock(LockComponent.Factory.class);
	private final LockWorkflow lockWorkflow = Mockito.mock(LockWorkflow.class);
	private final ErrorComponent.Factory errorWindowFactory = Mockito.mock(ErrorComponent.Factory.class);
	private final ExecutorService executor = Mockito.mock(ExecutorService.class);
	private final List<Runnable> queuedTasks = new ArrayList<>();
	private MockedStatic<Platform> platform;
	private FxApplicationWindows appWindows;

	@BeforeEach
	public void setup() {
		platform = Mockito.mockStatic(Platform.class);
		platform.when(() -> Platform.runLater(Mockito.any())).thenAnswer(invocation -> {
			invocation.getArgument(0, Runnable.class).run();
			return null;
		});
		Mockito.doAnswer(invocation -> queuedTasks.add(invocation.getArgument(0))).when(executor).execute(Mockito.any());
		Mockito.when(vault.getDisplayName()).thenReturn("vault");
		var unlockComponent = Mockito.mock(UnlockComponent.class);
		Mockito.when(unlockComponent.unlockWorkflow()).thenReturn(unlockWorkflow);
		Mockito.when(unlockWorkflowFactory.create(Mockito.eq(vault), Mockito.any())).thenReturn(unlockComponent);
		var lockComponent = Mockito.mock(LockComponent.class);
		Mockito.when(lockComponent.lockWorkflow()).thenReturn(lockWorkflow);
		Mockito.when(lockWorkflowFactory.create(Mockito.eq(vault), Mockito.any())).thenReturn(lockComponent);
		Mockito.when(errorWindowFactory.create(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(ErrorComponent.class));
		appWindows = new FxApplicationWindows(primaryStage, Optional.empty(), Mockito.mock(MainWindowComponent.Builder.class), Mockito.mock(PreferencesComponent.Builder.class), Mockito.mock(QuitComponent.Builder.class), unlockWorkflowFactory, Mockito.mock(UpdateReminderComponent.Factory.class), lockWorkflowFactory, errorWindowFactory, Mockito.mock(VaultOptionsComponent.Factory.class), Mockito.mock(ShareVaultComponent.Factory.class), Mockito.mock(EventViewComponent.Factory.class), Mockito.mock(NotificationComponent.Factory.class), executor, Mockito.mock(Dialogs.class));
	}

	@AfterEach
	public void tearDown() {
		platform.close();
	}

	private VaultState givenVaultState(VaultState.Value value) {
		var state = new VaultState(value);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		return state;
	}

	private void runQueuedTasks() {
		var tasks = List.copyOf(queuedTasks);
		queuedTasks.clear();
		tasks.forEach(Runnable::run);
	}

	private static void assertCompletedNormally(CompletionStage<Void> stage) {
		var future = stage.toCompletableFuture();
		Assertions.assertTrue(future.isDone());
		Assertions.assertFalse(future.isCompletedExceptionally());
	}

	@Test
	public void testSecondUnlockStartWhileProcessingSkips() {
		var state = givenVaultState(VaultState.Value.LOCKED);

		var firstUnlock = appWindows.startUnlockWorkflow(vault, null);
		var secondUnlock = appWindows.startUnlockWorkflow(vault, null);
		runQueuedTasks();

		Mockito.verify(unlockWorkflowFactory).create(vault, null);
		Mockito.verify(unlockWorkflow).run();
		Mockito.verifyNoInteractions(errorWindowFactory);
		assertCompletedNormally(firstUnlock);
		assertCompletedNormally(secondUnlock);
		Assertions.assertEquals(VaultState.Value.PROCESSING, state.get());
	}

	@Test
	public void testStaleLockStartSkips() {
		var state = givenVaultState(VaultState.Value.LOCKED);

		var lock = appWindows.startLockWorkflow(vault, null);
		runQueuedTasks();

		Mockito.verifyNoInteractions(lockWorkflowFactory, errorWindowFactory);
		assertCompletedNormally(lock);
		Assertions.assertEquals(VaultState.Value.LOCKED, state.get());
	}

	@Test
	public void testUnlockCompletesAfterWorkflowRan() {
		givenVaultState(VaultState.Value.LOCKED);

		var unlock = appWindows.startUnlockWorkflow(vault, owner);

		Mockito.verify(unlockWorkflowFactory).create(vault, owner);
		Assertions.assertFalse(unlock.toCompletableFuture().isDone());
		Mockito.verify(unlockWorkflow, Mockito.never()).run();
		runQueuedTasks();
		Mockito.verify(unlockWorkflow).run();
		assertCompletedNormally(unlock);
	}

	@Test
	public void testLockCompletesAfterWorkflowRan() {
		givenVaultState(VaultState.Value.UNLOCKED);

		var lock = appWindows.startLockWorkflow(vault, owner);

		Mockito.verify(lockWorkflowFactory).create(vault, owner);
		Assertions.assertFalse(lock.toCompletableFuture().isDone());
		Mockito.verify(lockWorkflow, Mockito.never()).run();
		runQueuedTasks();
		Mockito.verify(lockWorkflow).run();
		assertCompletedNormally(lock);
	}

	@Test
	public void testUnlockFactoryFailureShowsErrorInOwner() {
		givenVaultState(VaultState.Value.LOCKED);
		Mockito.when(unlockWorkflowFactory.create(vault, owner)).thenThrow(new RuntimeException("factory failed"));

		appWindows.startUnlockWorkflow(vault, owner);
		runQueuedTasks();

		Mockito.verify(errorWindowFactory).create(Mockito.any(), Mockito.eq(owner), Mockito.isNull());
	}

	@Test
	public void testUnlockFactoryFailureWithoutOwnerShowsErrorInPrimaryStage() {
		givenVaultState(VaultState.Value.LOCKED);
		Mockito.when(unlockWorkflowFactory.create(vault, null)).thenThrow(new RuntimeException("factory failed"));

		appWindows.startUnlockWorkflow(vault, null);
		runQueuedTasks();

		Mockito.verify(errorWindowFactory).create(Mockito.any(), Mockito.eq(primaryStage), Mockito.isNull());
	}

	@Test
	public void testLockFactoryFailureShowsErrorInOwner() {
		givenVaultState(VaultState.Value.UNLOCKED);
		Mockito.when(lockWorkflowFactory.create(vault, owner)).thenThrow(new RuntimeException("factory failed"));

		appWindows.startLockWorkflow(vault, owner);
		runQueuedTasks();

		Mockito.verify(errorWindowFactory).create(Mockito.any(), Mockito.eq(owner), Mockito.isNull());
	}

	@Test
	public void testLockFactoryFailureWithoutOwnerShowsErrorInPrimaryStage() {
		givenVaultState(VaultState.Value.UNLOCKED);
		Mockito.when(lockWorkflowFactory.create(vault, null)).thenThrow(new RuntimeException("factory failed"));

		appWindows.startLockWorkflow(vault, null);
		runQueuedTasks();

		Mockito.verify(errorWindowFactory).create(Mockito.any(), Mockito.eq(primaryStage), Mockito.isNull());
	}

}
