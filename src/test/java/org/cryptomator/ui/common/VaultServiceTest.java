package org.cryptomator.ui.common;

import org.cryptomator.JavaFXUtil;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javafx.application.Platform;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class VaultServiceTest {

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final VaultService vaultService = new VaultService(Mockito.mock(RevealPathService.class), executor);
	private final List<Boolean> settledOnFxThread = new CopyOnWriteArrayList<>();

	@BeforeAll
	public static void startup() throws InterruptedException {
		var isRunning = JavaFXUtil.startPlatform();
		Assumptions.assumeTrue(isRunning);
	}

	@AfterEach
	public void tearDown() {
		executor.shutdownNow();
	}

	private Vault mockVault(VaultState state) {
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.doAnswer(_ -> {
			settledOnFxThread.add(Platform.isFxApplicationThread());
			return null;
		}).when(vault).finishProcessing();
		return vault;
	}

	@Test
	public void testLockAllSkipsVaultNotUnlocked() throws Exception {
		var vault = mockVault(new VaultState(VaultState.Value.LOCKED));

		var lockAllTask = vaultService.createLockAllTask(List.of(vault), false);
		executor.execute(lockAllTask);

		Assertions.assertEquals(List.of(vault), lockAllTask.get(1, TimeUnit.SECONDS));
		Mockito.verify(vault, Mockito.never()).lock(Mockito.anyBoolean());
		Mockito.verify(vault, Mockito.never()).finishProcessing();
	}

	@Test
	public void testLockAllWaitsForBusyVault() throws Exception {
		var state = new VaultState(VaultState.Value.PROCESSING);
		var vault = mockVault(state);

		var lockAllTask = vaultService.createLockAllTask(List.of(vault), false);
		executor.execute(lockAllTask);

		Assertions.assertThrows(TimeoutException.class, () -> lockAllTask.get(500, TimeUnit.MILLISECONDS));
		Mockito.verify(vault, Mockito.never()).lock(Mockito.anyBoolean());
		state.transition(VaultState.Value.PROCESSING, VaultState.Value.UNLOCKED);
		lockAllTask.get(1, TimeUnit.SECONDS);

		var inOrder = Mockito.inOrder(vault);
		inOrder.verify(vault).lock(false);
		inOrder.verify(vault).finishProcessing();
		Assertions.assertEquals(List.of(false), settledOnFxThread);
	}

	@Test
	public void testLockAllWaitsForAllTasksBeforeFailing() throws Exception {
		var a = mockVault(new VaultState(VaultState.Value.UNLOCKED));
		var b = mockVault(new VaultState(VaultState.Value.UNLOCKED));
		var lockFailure = new UnmountFailedException("unmount failed");
		Mockito.doThrow(lockFailure).when(a).lock(false);
		var lockOfBReleased = new CountDownLatch(1);
		Mockito.doAnswer(_ -> {
			lockOfBReleased.await();
			return null;
		}).when(b).lock(false);

		var lockAllTask = vaultService.createLockAllTask(List.of(a, b), false);
		executor.execute(lockAllTask);

		Assertions.assertThrows(TimeoutException.class, () -> lockAllTask.get(500, TimeUnit.MILLISECONDS));
		lockOfBReleased.countDown();
		var thrown = Assertions.assertThrows(ExecutionException.class, () -> lockAllTask.get(1, TimeUnit.SECONDS));

		Assertions.assertSame(lockFailure, thrown.getCause().getCause());
		Mockito.verify(a).finishProcessing();
		Mockito.verify(b).finishProcessing();
		Assertions.assertEquals(List.of(false, false), settledOnFxThread);
	}

}
