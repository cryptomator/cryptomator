package org.cryptomator.ui.launcher;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.fxapp.FxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

@Singleton
class AppLaunchEventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(AppLaunchEventHandler.class);

	private final BlockingQueue<AppLaunchEvent> launchEventQueue;
	private final ExecutorService executorService;
	private final FxApplicationStarter fxApplicationStarter;
	private final VaultFactory vaultFactory;
	private final ObservableList<Vault> vaults;

	@Inject
	public AppLaunchEventHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue, ExecutorService executorService, FxApplicationStarter fxApplicationStarter, VaultFactory vaultFactory, ObservableList<Vault> vaults) {
		this.launchEventQueue = launchEventQueue;
		this.executorService = executorService;
		this.fxApplicationStarter = fxApplicationStarter;
		this.vaultFactory = vaultFactory;
		this.vaults = vaults;
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
			case REVEAL_APP:
				fxApplicationStarter.get(hasTrayIcon).thenAccept(FxApplication::showMainWindow);
				break;
			case OPEN_FILE:
				fxApplicationStarter.get(hasTrayIcon).thenRun(() -> {
					Platform.runLater(() -> {
						event.getPathsToOpen().forEach(this::addVault);
					});
				});
				break;
			default:
				LOG.warn("Unsupported event type: {}", event.getType());
				break;
		}
	}

	// TODO dedup MainWindowController...
	private void addVault(Path potentialVaultPath) {
		assert Platform.isFxApplicationThread();
		// TODO CryptoFileSystemProvider.containsVault(potentialVaultPath, "masterkey.cryptomator");
		VaultSettings settings = VaultSettings.withRandomId();
		settings.path().set(potentialVaultPath);
		Vault vault = vaultFactory.get(settings);
		vaults.add(vault);
		LOG.debug("Added vault {}", potentialVaultPath);
	}

}
