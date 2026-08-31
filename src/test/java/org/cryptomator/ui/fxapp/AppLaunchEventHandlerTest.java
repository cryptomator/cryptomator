package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.launcher.AppLaunchEvent;
import org.cryptomator.launcher.RevealRunningEvent;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.dialogs.Dialogs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.stage.Stage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class AppLaunchEventHandlerTest {

	private BlockingQueue<AppLaunchEvent> queue;
	private ExecutorService executor;
	private FxApplicationWindows appWindows;
	private AppLaunchEventHandler handler;

	@BeforeEach
	public void setup() {
		queue = new LinkedBlockingQueue<>();
		executor = Executors.newSingleThreadExecutor();
		appWindows = mock(FxApplicationWindows.class);
		handler = new AppLaunchEventHandler(queue, executor, appWindows, mock(VaultListManager.class), FXCollections.observableArrayList(), mock(VaultService.class), mock(Stage.class), mock(Dialogs.class));
	}

	@AfterEach
	public void teardown() {
		executor.shutdownNow();
	}

	// TODO: Add test for OpenHubVaultEvent once the event opens the hub vault flow for real.

	@Test
	@DisplayName("a RevealRunningEvent reveals the main window")
	public void testRevealRunningEventShowsMainWindow() {
		queue.add(new RevealRunningEvent());

		handler.startHandlingLaunchEvents();

		verify(appWindows, timeout(2000)).showMainWindow();
	}

}
