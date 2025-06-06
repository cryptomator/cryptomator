package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.launcher.AppLaunchEvent;
import org.cryptomator.ui.common.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_EXT;

// TODO: use message bus
@FxApplicationScoped
class AppLaunchEventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AppLaunchEventHandler.class);

	private final BlockingQueue<AppLaunchEvent> launchEventQueue;
	private final ExecutorService executorService;
	private final FxApplicationWindows appWindows;
	private final VaultListManager vaultListManager;
	private final VaultService vaultService;

	@Inject
	public AppLaunchEventHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue, ExecutorService executorService, FxApplicationWindows appWindows, VaultListManager vaultListManager, VaultService vaultService) {
		this.launchEventQueue = launchEventQueue;
		this.executorService = executorService;
		this.appWindows = appWindows;
		this.vaultListManager = vaultListManager;
		this.vaultService = vaultService;
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
		switch (event.type()) {
			case REVEAL_APP -> appWindows.showMainWindow();
			case OPEN_FILE -> Platform.runLater(() -> {
				event.pathsToOpen().forEach(this::openPotentialVault);
			});
			default -> LOG.warn("Unsupported event type: {}", event.type());
		}
	}

	// TODO deduplicate MainWindowController...
	private void openPotentialVault(Path path) {
		assert Platform.isFxApplicationThread();
		try {
			Path potentialVaultPath = path.getFileName().toString().endsWith(CRYPTOMATOR_FILENAME_EXT) ? path.getParent() : path;
			final Optional<Vault> v = vaultListManager.get(potentialVaultPath);
			if (v.isPresent()) {
				if (v.get().isUnlocked()) {
					vaultService.reveal(v.get());
				} else if (v.get().isLocked()) {
					appWindows.startUnlockWorkflow(v.get(), null);
				}
			} else {
				vaultListManager.add(potentialVaultPath);
				LOG.debug("Added vault {}", potentialVaultPath);
			}
		} catch (IOException e) {
			LOG.error("Failed to add vault " + path, e);
		}
	}

}
