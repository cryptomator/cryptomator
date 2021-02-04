package org.cryptomator.ui.launcher;

import org.cryptomator.common.ShutdownHook;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.QuitResponse;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class AppLifecycleListener {

	private static final Logger LOG = LoggerFactory.getLogger(AppLifecycleListener.class);
	public static final Set<VaultState> STATES_ALLOWING_TERMINATION = EnumSet.of(VaultState.LOCKED, VaultState.NEEDS_MIGRATION, VaultState.MISSING, VaultState.ERROR);

	private final FxApplicationStarter fxApplicationStarter;
	private final CountDownLatch shutdownLatch;
	private final ObservableList<Vault> vaults;
	private final AtomicBoolean allowQuitWithoutPrompt;

	@Inject
	AppLifecycleListener(FxApplicationStarter fxApplicationStarter, @Named("shutdownLatch") CountDownLatch shutdownLatch, ShutdownHook shutdownHook, ObservableList<Vault> vaults) {
		this.fxApplicationStarter = fxApplicationStarter;
		this.shutdownLatch = shutdownLatch;
		this.vaults = vaults;
		this.allowQuitWithoutPrompt = new AtomicBoolean(true);
		vaults.addListener(this::vaultListChanged);

		// register preferences shortcut
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::showPreferencesWindow);
		}

		// register preferences shortcut
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)) {
			Desktop.getDesktop().setAboutHandler(this::showAboutWindow);
		}

		// register quit handler
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
			Desktop.getDesktop().setQuitHandler(this::handleQuitRequest);
		}

		shutdownHook.runOnShutdown(this::forceUnmountRemainingVaults);
	}

	/**
	 * Gracefully terminates the application.
	 */
	public void quit() {
		handleQuitRequest(null, new QuitResponse() {
			@Override
			public void performQuit() {
				System.exit(0);
			}

			@Override
			public void cancelQuit() {
				// no-op
			}
		});
	}

	private void handleQuitRequest(@SuppressWarnings("unused") EventObject e, QuitResponse response) {
		QuitResponse decoratedQuitResponse = decorateQuitResponse(response);
		if (allowQuitWithoutPrompt.get()) {
			decoratedQuitResponse.performQuit();
		} else {
			fxApplicationStarter.get().thenAccept(app -> app.showQuitWindow(decoratedQuitResponse));
		}
	}

	private QuitResponse decorateQuitResponse(QuitResponse originalQuitResponse) {
		return new QuitResponse() {
			@Override
			public void performQuit() {
				Platform.exit(); // will be no-op, if JavaFX never started.
				shutdownLatch.countDown(); // main thread is waiting for this latch
				EventQueue.invokeLater(originalQuitResponse::performQuit); // this will eventually call System.exit(0)
			}

			@Override
			public void cancelQuit() {
				originalQuitResponse.cancelQuit();
			}
		};
	}

	private void vaultListChanged(@SuppressWarnings("unused") Observable observable) {
		assert Platform.isFxApplicationThread();
		boolean allVaultsAllowTermination = vaults.stream().map(Vault::getState).allMatch(STATES_ALLOWING_TERMINATION::contains);
		boolean suddenTerminationChanged = allowQuitWithoutPrompt.compareAndSet(!allVaultsAllowTermination, allVaultsAllowTermination);
		if (suddenTerminationChanged) {
			LOG.debug("Allow quitting without prompt: {}", allVaultsAllowTermination);
		}
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		fxApplicationStarter.get().thenAccept(app -> app.showPreferencesWindow(SelectedPreferencesTab.ANY));
	}

	private void showAboutWindow(@SuppressWarnings("unused") AboutEvent aboutEvent) {
		fxApplicationStarter.get().thenAccept(app -> app.showPreferencesWindow(SelectedPreferencesTab.ABOUT));
	}

	private void forceUnmountRemainingVaults() {
		for (Vault vault : vaults) {
			if (vault.isUnlocked()) {
				try {
					vault.lock(true);
				} catch (Volume.VolumeException e) {
					LOG.error("Failed to unmount vault " + vault.getPath(), e);
				}
			}
		}
	}

}
