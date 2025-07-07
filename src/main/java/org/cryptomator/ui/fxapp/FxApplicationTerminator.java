package org.cryptomator.ui.fxapp;

import com.google.common.base.Preconditions;
import org.cryptomator.common.ShutdownHook;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.cryptomator.ui.common.VaultService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.awt.Desktop;
import java.awt.desktop.QuitResponse;
import java.awt.desktop.QuitStrategy;
import java.io.IOException;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.cryptomator.common.vaults.VaultState.Value.*;

@FxApplicationScoped
public class FxApplicationTerminator {

	private static final Set<VaultState.Value> STATES_ALLOWING_TERMINATION = EnumSet.of(LOCKED, NEEDS_MIGRATION, MISSING, ERROR, VAULT_CONFIG_MISSING, ALL_MISSING);
	private static final Set<VaultState.Value> STATES_PREVENT_TERMINATION = EnumSet.of(PROCESSING);
	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationTerminator.class);

	private final ObservableList<Vault> vaults;
	private final ShutdownHook shutdownHook;
	private final FxApplicationWindows appWindows;
	private final AtomicBoolean allowQuitWithoutPrompt = new AtomicBoolean();
	private final AtomicBoolean preventQuitWithGracefulLock = new AtomicBoolean();
	private final Settings settings;
	private final VaultService vaultService;

	@Inject
	public FxApplicationTerminator(ObservableList<Vault> vaults, ShutdownHook shutdownHook, FxApplicationWindows appWindows, Settings settings, VaultService vaultService) {
		this.vaults = vaults;
		this.shutdownHook = shutdownHook;
		this.appWindows = appWindows;
		this.settings = settings;
		this.vaultService = vaultService;
	}

	public void initialize() {
		Preconditions.checkState(Desktop.isDesktopSupported(), "java.awt.Desktop not supported");
		Desktop desktop = Desktop.getDesktop();

		// register quit handler
		if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
			desktop.setQuitHandler(this::handleQuitRequest);
		}

		// set quit strategy (cmd+q would call `System.exit(0)` otherwise)
		if (desktop.isSupported(Desktop.Action.APP_QUIT_STRATEGY)) {
			desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
		}

		// allow sudden termination?
		vaultListChanged(vaults);
		vaults.addListener(this::vaultListChanged);

		shutdownHook.runOnShutdown(this::forceUnmountRemainingVaults);
	}

	/**
	 * Gracefully terminates the application.
	 */
	public void terminate() {
		handleQuitRequest(null, new NoopQuitResponse());
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		boolean allowSuddenTermination = vaults.stream().map(Vault::getState).allMatch(STATES_ALLOWING_TERMINATION::contains);
		boolean stateChanged = allowQuitWithoutPrompt.compareAndSet(!allowSuddenTermination, allowSuddenTermination);

		boolean preventGracefulTermination = vaults.stream().map(Vault::getState).anyMatch(STATES_PREVENT_TERMINATION::contains);
		preventQuitWithGracefulLock.set(preventGracefulTermination);

		Desktop desktop = Desktop.getDesktop();
		if (stateChanged && desktop.isSupported(Desktop.Action.APP_SUDDEN_TERMINATION)) {
			if (allowSuddenTermination) {
				LOG.debug("Enabling sudden termination");
				desktop.enableSuddenTermination();
			} else {
				LOG.debug("Disabling sudden termination");
				desktop.disableSuddenTermination();
			}
		}
	}

	/**
	 * Asks the app to quit. If confirmed, the JavaFX application will exit before giving a {@code response}.
	 *
	 * @param e ignored
	 * @param response a quit response that will be {@link ExitingQuitResponse decorated in order to exit the JavaFX application}.
	 */
	private void handleQuitRequest(@SuppressWarnings("unused") @Nullable EventObject e, QuitResponse response) {
		var exitingResponse = new ExitingQuitResponse(response);

		if (allowQuitWithoutPrompt.get()) {
			exitingResponse.performQuit();
		} else if (settings.autoCloseVaults.get() && !preventQuitWithGracefulLock.get()) {
			var lockAllTask = vaultService.createLockAllTask(vaults.filtered(Vault::isUnlocked), false);
			lockAllTask.setOnSucceeded(event -> {
				LOG.info("Locked remaining vaults was succesful.");
				exitingResponse.performQuit();
			});
			lockAllTask.setOnFailed(event -> {
				LOG.warn("Unable to lock all vaults.");
				appWindows.showQuitWindow(exitingResponse, true);
			});
			lockAllTask.run();
		} else {
			appWindows.showQuitWindow(exitingResponse, false);
		}
	}

	private void forceUnmountRemainingVaults() {
		for (Vault vault : vaults) {
			if (vault.isUnlocked()) {
				try {
					vault.lock(true);
				} catch (UnmountFailedException | IOException e) {
					LOG.error("Failed to unmount vault " + vault.getPath(), e);
				}
			}
		}
	}

	/**
	 * A dummy QuitResponse that ignores the response.
	 * <p>
	 * To be used with {@link #handleQuitRequest(EventObject, QuitResponse)} if the invoking method is not interested in the response.
	 */
	private static class NoopQuitResponse implements QuitResponse {

		@Override
		public void performQuit() {
			// no-op
		}

		@Override
		public void cancelQuit() {
			// no-op
		}
	}

}
