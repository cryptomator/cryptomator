package org.cryptomator.ui.fxapp;

import dagger.Lazy;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.stage.Stage;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
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
import java.awt.desktop.QuitResponse;
import java.util.Optional;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

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

	@Inject
	FxApplication(Settings settings, Lazy<MainWindowComponent> mainWindow, Lazy<PreferencesComponent> preferencesWindow, UnlockComponent.Builder unlockWindowBuilder, QuitComponent.Builder quitWindowBuilder, Optional<MacFunctions> macFunctions, VaultService vaultService, LicenseHolder licenseHolder) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.unlockWindowBuilder = unlockWindowBuilder;
		this.quitWindowBuilder = quitWindowBuilder;
		this.macFunctions = macFunctions;
		this.vaultService = vaultService;
		this.licenseHolder = licenseHolder;
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		Platform.setImplicitExit(false);

		hasVisibleStages.addListener(this::hasVisibleStagesChanged);

		settings.theme().addListener(this::themeChanged);
		loadSelectedStyleSheet(settings.theme().get());
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

	public void showPreferencesWindow(SelectedPreferencesTab selectedTab) {
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

	public void showQuitWindow(QuitResponse response) {
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

}
