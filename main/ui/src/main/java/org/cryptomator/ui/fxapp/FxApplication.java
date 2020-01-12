package org.cryptomator.ui.fxapp;

import dagger.Lazy;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.stage.Stage;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.ShutdownHook;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.jni.JniException;
import org.cryptomator.jni.MacApplicationUiAppearance;
import org.cryptomator.jni.MacApplicationUiState;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.Desktop;
import java.awt.desktop.QuitResponse;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);
	private static final Set<VaultState> STATES_ALLOWING_TERMINATION = EnumSet.of(VaultState.LOCKED, VaultState.NEEDS_MIGRATION, VaultState.MISSING, VaultState.ERROR);

	private final Settings settings;
	private final Lazy<MainWindowComponent> mainWindow;
	private final Lazy<PreferencesComponent> preferencesWindow;
	private final UnlockComponent.Builder unlockWindowBuilder;
	private final QuitComponent.Builder quitWindowBuilder;
	private final Optional<MacFunctions> macFunctions;
	private final VaultService vaultService;
	private final LicenseHolder licenseHolder;
	private final ObservableSet<Stage> visibleStages = FXCollections.observableSet();
	private final BooleanBinding hasVisibleStages = Bindings.isNotEmpty(visibleStages);
	private final AtomicBoolean allowSuddenTermination;
	private final CountDownLatch shutdownLatch;
	private final ShutdownHook shutdownHook;
	private final ObservableList<Vault> vaults;

	@Inject
	FxApplication(Settings settings, Lazy<MainWindowComponent> mainWindow, Lazy<PreferencesComponent> preferencesWindow, UnlockComponent.Builder unlockWindowBuilder, QuitComponent.Builder quitWindowBuilder, Optional<MacFunctions> macFunctions, VaultService vaultService, LicenseHolder licenseHolder, ObservableList<Vault> vaults, @Named("shutdownLatch") CountDownLatch shutdownLatch, ShutdownHook shutdownHook) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.unlockWindowBuilder = unlockWindowBuilder;
		this.quitWindowBuilder = quitWindowBuilder;
		this.macFunctions = macFunctions;
		this.vaultService = vaultService;
		this.licenseHolder = licenseHolder;
		this.vaults = vaults;
		this.shutdownLatch = shutdownLatch;
		this.shutdownHook = shutdownHook;
		this.allowSuddenTermination = new AtomicBoolean(true);
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		Platform.setImplicitExit(false);

		hasVisibleStages.addListener(this::hasVisibleStagesChanged);

		settings.theme().addListener(this::themeChanged);
		loadSelectedStyleSheet(settings.theme().get());

		vaults.addListener(this::vaultsChanged);

		shutdownHook.runOnShutdown(this::forceUnmountRemainingVaults);

		// register preferences shortcut
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::showPreferencesWindow);
		}

		// register quit handler
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
			Desktop.getDesktop().setQuitHandler(this::handleQuitRequest);
		}

		// allow sudden termination
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_SUDDEN_TERMINATION)) {
			Desktop.getDesktop().enableSuddenTermination();
		}
	}

	@Override
	public void start(Stage stage) {
		throw new UnsupportedOperationException("Use start() instead.");
	}

	private void addVisibleStage(Stage stage) {
		visibleStages.add(stage);
		stage.setOnHidden(evt -> visibleStages.remove(stage));
	}

	private void hasVisibleStagesChanged(@SuppressWarnings("unused") ObservableValue<? extends Boolean> observableValue, @SuppressWarnings("unused") boolean oldValue, boolean newValue) {
		if (newValue) {
			macFunctions.map(MacFunctions::uiState).ifPresent(MacApplicationUiState::transformToForegroundApplication);
		} else {
			macFunctions.map(MacFunctions::uiState).ifPresent(MacApplicationUiState::transformToAgentApplication);
		}
	}

	public void showPreferencesTab(SelectedPreferencesTab selectedTab) {
		Platform.runLater(() -> {
			Stage stage = preferencesWindow.get().showPreferencesWindow(selectedTab);
			addVisibleStage(stage);
			LOG.debug("Showing Preferences");
		});
	}

	public void showMainWindow() {
		Platform.runLater(() -> {
			Stage stage = mainWindow.get().showMainWindow();
			addVisibleStage(stage);
			LOG.debug("Showing MainWindow");
		});
	}

	public void showUnlockWindow(Vault vault) {
		Platform.runLater(() -> {
			Stage stage = unlockWindowBuilder.vault(vault).build().showUnlockWindow();
			addVisibleStage(stage);
			LOG.debug("Showing UnlockWindow for {}", vault.getDisplayableName());
		});
	}

	private void showQuitWindow(@SuppressWarnings("unused") EventObject actionEvent, QuitResponse response) {
		Platform.runLater(() -> {
			Stage stage = quitWindowBuilder.quitResponse(response).build().showQuitWindow();
			addVisibleStage(stage);
			LOG.debug("Showing QuitWindow");
		});
	}

	public VaultService getVaultService() {
		return vaultService;
	}

	private void themeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, @SuppressWarnings("unused") UiTheme oldValue, UiTheme newValue) {
		loadSelectedStyleSheet(newValue);
	}

	private void loadSelectedStyleSheet(UiTheme desiredTheme) {
		UiTheme theme = licenseHolder.isValidLicense() ? desiredTheme : UiTheme.LIGHT;
		switch (theme) {
//			case CUSTOM:
//				// TODO
//				Application.setUserAgentStylesheet(getClass().getResource("/css/mac_theme.css").toString());
//				break;
			case DARK:
				Application.setUserAgentStylesheet(getClass().getResource("/css/dark_theme.css").toString());
				macFunctions.map(MacFunctions::uiAppearance).ifPresent(JniException.ignore(MacApplicationUiAppearance::setToDarkAqua));
				break;
			case LIGHT:
			default:
				Application.setUserAgentStylesheet(getClass().getResource("/css/light_theme.css").toString());
				macFunctions.map(MacFunctions::uiAppearance).ifPresent(JniException.ignore(MacApplicationUiAppearance::setToAqua));
				break;
		}
	}

	public void quitApplication() {
		handleQuitRequest(null, new QuitResponse() {
			@Override
			public void performQuit() {
				shutdownLatch.countDown();
			}

			@Override
			public void cancelQuit() {
				// no-op
			}
		});
	}

	private void showPreferencesWindow(@SuppressWarnings("unused") EventObject actionEvent) {
		showPreferencesTab(SelectedPreferencesTab.ANY);
	}

	private void handleQuitRequest(EventObject e, QuitResponse response) {
		if (allowSuddenTermination.get()) {
			response.performQuit(); // really?
		} else {
			showQuitWindow(e, response);
		}
	}

	private void vaultsChanged(@SuppressWarnings("unused") Observable observable) {
		boolean allVaultsAllowTermination = vaults.stream().map(Vault::getState).allMatch(STATES_ALLOWING_TERMINATION::contains);
		boolean suddenTerminationChanged = allowSuddenTermination.compareAndSet(!allVaultsAllowTermination, allVaultsAllowTermination);
		if (suddenTerminationChanged && Desktop.getDesktop().isSupported(Desktop.Action.APP_SUDDEN_TERMINATION)) {
			if (allVaultsAllowTermination) {
				Desktop.getDesktop().enableSuddenTermination();
				LOG.debug("sudden termination enabled");
			} else {
				Desktop.getDesktop().disableSuddenTermination();
				LOG.debug("sudden termination disabled");
			}
		}
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
