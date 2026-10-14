package org.cryptomator.ui.lock;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

class LockWorkflowTest {

	private final Vault vault = Mockito.mock(Vault.class);
	private final AtomicReference<CompletableFuture<Boolean>> forceRetryDecision = new AtomicReference<>();
	private MockedStatic<Platform> platform;
	private LockWorkflow workflow;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		platform = Mockito.mockStatic(Platform.class);
		workflow = new LockWorkflow(Mockito.mock(Stage.class), vault, forceRetryDecision, Mockito.mock(Lazy.class), Mockito.mock(Lazy.class), Mockito.mock(FxApplicationWindows.class));
	}

	@AfterEach
	public void tearDown() {
		platform.close();
	}

	@Test
	public void testLockSettlesAfterLock() throws Exception {
		workflow.call();

		var inOrder = Mockito.inOrder(vault);
		inOrder.verify(vault).lock(false);
		inOrder.verify(vault).finishProcessing();
	}

	@Test
	public void testFailedLockSettles() throws Exception {
		Mockito.doThrow(IOException.class).when(vault).lock(false);

		Assertions.assertThrows(IOException.class, workflow::call);

		Mockito.verify(vault).finishProcessing();
	}

	@Test
	public void testCancelledForceDecisionSettles() throws Exception {
		Mockito.doThrow(UnmountFailedException.class).when(vault).lock(false);
		platform.when(() -> Platform.runLater(Mockito.any())).thenAnswer(_ -> {
			forceRetryDecision.get().cancel(true); // the user closes the force dialog
			return null;
		});

		workflow.call();

		Assertions.assertTrue(workflow.isCancelled());
		Mockito.verify(vault, Mockito.never()).lock(true);
		Mockito.verify(vault).finishProcessing();
	}

}
