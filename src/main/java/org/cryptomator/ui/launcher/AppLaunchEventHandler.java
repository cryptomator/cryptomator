package org.cryptomator.ui.launcher;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.fxapp.FxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javafx.application.Platform;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_EXT;

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

	public void startHandlingLaunchEvents() {
		executorService.submit(this::handleLaunchEvents);
	}

	private void handleLaunchEvents() {
		try {
			while (!Thread.interrupted()) {
				AppLaunchEvent event = launchEventQueue.take();
				handleLaunchEvent(event);
			}
		} catch (InterruptedException e) {
			LOG.warn("Interrupted launch event handler.");
			Thread.currentThread().interrupt();
		}
	}

	private void handleLaunchEvent(AppLaunchEvent event) {
		switch (event.getType()) {
			case REVEAL_APP -> fxApplicationStarter.get().thenAccept(FxApplication::showMainWindow);
			case OPEN_FILE -> fxApplicationStarter.get().thenRun(() -> {
				Platform.runLater(() -> {
					event.getPathsToOpen().forEach(this::addOrRevealVault);
				});
			});
			default -> LOG.warn("Unsupported event type: {}", event.getType());
		}
	}

	// TODO deduplicate MainWindowController...
	private void addOrRevealVault(Path potentialVaultPath) {
		assert Platform.isFxApplicationThread();
		try {
			final Vault v;
			if (potentialVaultPath.getFileName().toString().endsWith(CRYPTOMATOR_FILENAME_EXT)) {
				v = vaultListManager.add(potentialVaultPath.getParent());
			} else {
				v = vaultListManager.add(potentialVaultPath);
			}

			if (v.isUnlocked()) {
				fxApplicationStarter.get().thenAccept(app -> app.getVaultService().reveal(v));
			}
			LOG.debug("Added vault {}", potentialVaultPath);
		} catch (IOException e) {
			LOG.error("Failed to add vault " + potentialVaultPath, e);
		}
	}

}
