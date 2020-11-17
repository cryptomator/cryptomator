package org.cryptomator.ui.launcher;

import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.fxapp.FxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javafx.application.Platform;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@Singleton
class AppLaunchEventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AppLaunchEventHandler.class);

	private final BlockingQueue<AppLaunchEvent> launchEventQueue;
	private final ExecutorService executorService;
	private final FxApplicationStarter fxApplicationStarter;
	private final VaultListManager vaultListManager;

	@Inject
	public AppLaunchEventHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue, ExecutorService executorService, FxApplicationStarter fxApplicationStarter, VaultListManager vaultListManager) {
		this.launchEventQueue = launchEventQueue;
		this.executorService = executorService;
		this.fxApplicationStarter = fxApplicationStarter;
		this.vaultListManager = vaultListManager;
	}

	public void startHandlingLaunchEvents(boolean hasTrayIcon) {
		executorService.submit(() -> handleLaunchEvents(hasTrayIcon));
	}

	private void handleLaunchEvents(boolean hasTrayIcon) {
		try {
			while (!Thread.interrupted()) {
				AppLaunchEvent event = launchEventQueue.take();
				handleLaunchEvent(hasTrayIcon, event);
			}
		} catch (InterruptedException e) {
			LOG.warn("Interrupted launch event handler.");
			Thread.currentThread().interrupt();
		}
	}

	private void handleLaunchEvent(boolean hasTrayIcon, AppLaunchEvent event) {
		switch (event.getType()) {
			case REVEAL_APP -> fxApplicationStarter.get(hasTrayIcon).thenAccept(FxApplication::showMainWindow);
			case OPEN_FILE -> fxApplicationStarter.get(hasTrayIcon).thenRun(() -> {
				Platform.runLater(() -> {
					event.getPathsToOpen().forEach(this::addVault);
				});
			});
			default -> LOG.warn("Unsupported event type: {}", event.getType());
		}
	}

	// TODO dedup MainWindowController...
	private void addVault(Path potentialVaultPath) {
		assert Platform.isFxApplicationThread();
		try {
			if (potentialVaultPath.getFileName().toString().equals(MASTERKEY_FILENAME)) {
				vaultListManager.add(potentialVaultPath.getParent());
			} else {
				vaultListManager.add(potentialVaultPath);
			}
			LOG.debug("Added vault {}", potentialVaultPath);
		} catch (NoSuchFileException e) {
			LOG.error("Failed to add vault " + potentialVaultPath, e);
		}
	}

}
