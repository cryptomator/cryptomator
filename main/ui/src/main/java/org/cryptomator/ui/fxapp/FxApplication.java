package org.cryptomator.ui.fxapp;

import dagger.Lazy;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.lock.LockComponent;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.awt.desktop.QuitResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Settings settings;
	private final Lazy<MainWindowComponent> mainWindow;
	private final Lazy<PreferencesComponent> preferencesWindow;
	private final Lazy<QuitComponent> quitWindow;
	private final Provider<UnlockComponent.Builder> unlockWindowBuilderProvider;
	private final Provider<LockComponent.Builder> lockWindowBuilderProvider;
	private final Optional<TrayIntegrationProvider> trayIntegration;
	private final Optional<UiAppearanceProvider> appearanceProvider;
	private final VaultService vaultService;
	private final LicenseHolder licenseHolder;
	private final ObservableList<Window> visibleWindows;
	private final BooleanBinding hasVisibleWindows;
	private final UiAppearanceListener systemInterfaceThemeListener = this::systemInterfaceThemeChanged;

	@Inject
	FxApplication(Settings settings, Lazy<MainWindowComponent> mainWindow, Lazy<PreferencesComponent> preferencesWindow, Provider<UnlockComponent.Builder> unlockWindowBuilderProvider, Provider<LockComponent.Builder> lockWindowBuilderProvider, Lazy<QuitComponent> quitWindow, Optional<TrayIntegrationProvider> trayIntegration, Optional<UiAppearanceProvider> appearanceProvider, VaultService vaultService, LicenseHolder licenseHolder) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.unlockWindowBuilderProvider = unlockWindowBuilderProvider;
		this.lockWindowBuilderProvider = lockWindowBuilderProvider;
		this.quitWindow = quitWindow;
		this.trayIntegration = trayIntegration;
		this.appearanceProvider = appearanceProvider;
		this.vaultService = vaultService;
		this.licenseHolder = licenseHolder;
		this.visibleWindows = Stage.getWindows().filtered(Window::isShowing);
		this.hasVisibleWindows = Bindings.isNotEmpty(visibleWindows);
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		Platform.setImplicitExit(false);

		hasVisibleWindows.addListener(this::hasVisibleStagesChanged);

		settings.theme().addListener(this::appThemeChanged);
		loadSelectedStyleSheet(settings.theme().get());
	}

	@Override
	public void start(Stage stage) {
		throw new UnsupportedOperationException("Use start() instead.");
	}

	private void hasVisibleStagesChanged(@SuppressWarnings("unused") ObservableValue<? extends Boolean> observableValue, @SuppressWarnings("unused") boolean oldValue, boolean newValue) {
		LOG.warn("has visible stages: {}", newValue);
		if (newValue) {
			trayIntegration.ifPresent(TrayIntegrationProvider::restoredFromTray);
		} else {
			trayIntegration.ifPresent(TrayIntegrationProvider::minimizedToTray);
		}
	}

	public void showPreferencesWindow(SelectedPreferencesTab selectedTab) {
		Platform.runLater(() -> {
			preferencesWindow.get().showPreferencesWindow(selectedTab);
			LOG.debug("Showing Preferences");
		});
	}

	public CompletionStage<Stage> showMainWindow() {
		CompletableFuture<Stage> future = new CompletableFuture<>();
		Platform.runLater(() -> {
			var win = mainWindow.get().showMainWindow();
			LOG.debug("Showing MainWindow");
			future.complete(win);
		});
		return future;
	}

	public void startUnlockWorkflow(Vault vault, Optional<Stage> owner) {
		Platform.runLater(() -> {
			unlockWindowBuilderProvider.get().vault(vault).owner(owner).build().startUnlockWorkflow();
			LOG.debug("Showing UnlockWindow for {}", vault.getDisplayName());
		});
	}

	public void startLockWorkflow(Vault vault, Optional<Stage> owner) {
		Platform.runLater(() -> {
			lockWindowBuilderProvider.get().vault(vault).owner(owner).build().startLockWorkflow();
			LOG.debug("Start lock workflow for {}", vault.getDisplayName());
		});
	}

	public void showQuitWindow(QuitResponse response) {
		Platform.runLater(() -> {
			quitWindow.get().showQuitWindow(response);
			LOG.debug("Showing QuitWindow");
		});
	}

	public VaultService getVaultService() {
		return vaultService;
	}

	private void appThemeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, @SuppressWarnings("unused") UiTheme oldValue, UiTheme newValue) {
		appearanceProvider.ifPresent(appearanceProvider -> {
			try {
				appearanceProvider.removeListener(systemInterfaceThemeListener);
			} catch (UiAppearanceException e) {
				LOG.error("Failed to disable automatic theme switching.");
			}
		});
		loadSelectedStyleSheet(newValue);
	}

	private void loadSelectedStyleSheet(UiTheme desiredTheme) {
		UiTheme theme = licenseHolder.isValidLicense() ? desiredTheme : UiTheme.LIGHT;
		switch (theme) {
			case LIGHT -> applyLightTheme();
			case DARK -> applyDarkTheme();
			case AUTOMATIC -> {
				appearanceProvider.ifPresent(appearanceProvider -> {
					try {
						appearanceProvider.addListener(systemInterfaceThemeListener);
					} catch (UiAppearanceException e) {
						LOG.error("Failed to enable automatic theme switching.");
					}
				});
				applySystemTheme();
			}
		}
	}

	private void systemInterfaceThemeChanged(Theme theme) {
		switch (theme) {
			case LIGHT -> applyLightTheme();
			case DARK -> applyDarkTheme();
		}
	}

	private void applySystemTheme() {
		appearanceProvider.ifPresent(appearanceProvider -> {
			systemInterfaceThemeChanged(appearanceProvider.getSystemTheme());
		});
	}

	private void applyLightTheme() {
		Application.setUserAgentStylesheet(getClass().getResource("/css/light_theme.css").toString());
		appearanceProvider.ifPresent(appearanceProvider -> {
			appearanceProvider.adjustToTheme(Theme.LIGHT);
		});
	}

	private void applyDarkTheme() {
		Application.setUserAgentStylesheet(getClass().getResource("/css/dark_theme.css").toString());
		appearanceProvider.ifPresent(appearanceProvider -> {
			appearanceProvider.adjustToTheme(Theme.DARK);
		});
	}

}
