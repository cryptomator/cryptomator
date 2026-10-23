package org.cryptomator.ui.preferences;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.keychain.KeychainModule;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.autostart.ToggleAutoStartFailedException;
import org.cryptomator.integrations.common.NamedServiceProvider;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.integrations.quickaccess.QuickAccessService;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Stage window;
	private final Settings settings;
	private final Optional<AutoStartProvider> autoStartProvider;
	private final List<QuickAccessService> quickAccessServices;
	private final RevealPathService revealPathService;
	private final Environment environment;
	private final List<KeychainAccessProvider> keychainAccessProviders;
	private final KeychainManager keychain;
	private final ExecutorService backgroundExecutor;
	private final FxApplicationWindows appWindows;
	public CheckBox useKeychainCheckbox;
	public ChoiceBox<KeychainAccessProvider> keychainBackendChoiceBox;
	public CheckBox useQuickAccessCheckbox;
	public ChoiceBox<QuickAccessService> quickAccessServiceChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox autoCloseVaultsCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public Button resetTrustedHostsButton;
	public ToggleGroup nodeOrientation;

	private final BooleanProperty keychainMigrationRunning = new SimpleBooleanProperty();

	@Inject
	GeneralPreferencesController(@PreferencesWindow Stage window, //
								 Settings settings, //
								 Optional<AutoStartProvider> autoStartProvider, //
								 List<KeychainAccessProvider> keychainAccessProviders, //
								 KeychainManager keychain, //
								 RevealPathService revealPathService, //
								 Environment environment, //
								 FxApplicationWindows appWindows, //
								 ExecutorService backgroundExecutor) {
		this.window = window;
		this.settings = settings;
		this.autoStartProvider = autoStartProvider;
		this.keychainAccessProviders = keychainAccessProviders;
		this.keychain = keychain;
		this.backgroundExecutor = backgroundExecutor;
		this.quickAccessServices = QuickAccessService.get().toList();
		this.revealPathService = revealPathService;
		this.environment = environment;
		this.appWindows = appWindows;
	}

	@FXML
	public void initialize() {
		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden);
		autoCloseVaultsCheckbox.selectedProperty().bindBidirectional(settings.autoCloseVaults);
		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode);
		autoStartProvider.ifPresent(autoStart -> autoStartCheckbox.setSelected(autoStart.isEnabled()));

		keychainBackendChoiceBox.getItems().addAll(keychainAccessProviders);
		keychainBackendChoiceBox.setValue(KeychainModule.selectProvider(settings.keychainProvider.get(), keychainAccessProviders));
		keychainBackendChoiceBox.setConverter(new NamedServiceConverter<>());
		useKeychainCheckbox.selectedProperty().bindBidirectional(settings.useKeychain);
		keychainBackendChoiceBox.disableProperty().bind(useKeychainCheckbox.selectedProperty().not().or(keychainMigrationRunning));
		keychainBackendChoiceBox.valueProperty().addListener(this::changeKeychainProvider);

		useQuickAccessCheckbox.selectedProperty().bindBidirectional(settings.useQuickAccess);
		var quickAccessSettingsConverter = new ServiceToSettingsConverter<>(quickAccessServices);
		quickAccessServiceChoiceBox.getItems().addAll(quickAccessServices);
		quickAccessServiceChoiceBox.setValue(quickAccessSettingsConverter.fromString(settings.quickAccessService.get()));
		quickAccessServiceChoiceBox.setConverter(new NamedServiceConverter<>());
		quickAccessServiceChoiceBox.valueProperty().addListener((_, _, newService) -> settings.quickAccessService.set(quickAccessSettingsConverter.toString(newService)));
		quickAccessServiceChoiceBox.disableProperty().bind(useQuickAccessCheckbox.selectedProperty().not());
		if (resetTrustedHostsButton != null) {
			resetTrustedHostsButton.disableProperty().bind(Bindings.isEmpty(settings.trustedHosts));
		}
	}

	private void changeKeychainProvider(Observable observable, KeychainAccessProvider oldProvider, KeychainAccessProvider newProvider) {
		if (keychainMigrationRunning.get()) {
			return; // ignore the revert after a failed migration
		}
		//currently, we migrate on macOS (touchID vs regular keychain)
		//and on Linux (GNOME Keyring / KDE Wallet / Secret Service)
		if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			var idsAndNames = new HashMap<String, String>();
			settings.directories.forEach(vs -> idsAndNames.put(vs.id, vs.displayName.getValue()));
			idsAndNames.put(DeviceKey.KEYCHAIN_KEY, DeviceKey.KEYCHAIN_DISPLAY_NAME);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Migrating keychain entries {} from {} to {}", idsAndNames.keySet(), oldProvider.getName(), newProvider.getName());
			}
			keychainMigrationRunning.set(true);
			CompletableFuture.runAsync(() -> {
				try {
					KeychainManager.migrate(oldProvider, newProvider, idsAndNames);
				} catch (KeychainAccessException e) {
					throw new CompletionException(e);
				}
			}, backgroundExecutor).whenCompleteAsync((_, exception) -> {
				if (exception == null) {
					settings.keychainProvider.set(newProvider.getClass().getName());
				} else {
					LOG.error("Failed to migrate keychain entries from {} to {}", oldProvider.getName(), newProvider.getName(), exception);
					keychainBackendChoiceBox.setValue(oldProvider);
					appWindows.showErrorWindow(exception, window, window.getScene());
				}
				keychainMigrationRunning.set(false);
			}, Platform::runLater);
		} else {
			settings.keychainProvider.set(newProvider.getClass().getName());
		}
	}

	public boolean isAutoStartSupported() {
		return autoStartProvider.isPresent();
	}

	public boolean isHubTrustOnFirstUseEnabled() {
		return environment.hubTrustOnFirstUse();
	}

	@FXML
	public void toggleAutoStart() {
		autoStartProvider.ifPresent(autoStart -> {
			boolean enableAutoStart = autoStartCheckbox.isSelected();
			try {
				if (enableAutoStart) {
					autoStart.enable();
				} else {
					autoStart.disable();
				}
			} catch (ToggleAutoStartFailedException e) {
				autoStartCheckbox.setSelected(!enableAutoStart); // restore previous state
				LOG.error("Failed to toggle autostart.", e);
				appWindows.showErrorWindow(e, window, window.getScene());
			}
		});
	}

	public boolean isSomeQuickAccessServiceAvailable() {
		return !quickAccessServices.isEmpty();
	}

	@FXML
	public void resetTrustedHosts() {
		settings.trustedHosts.clear();
	}

	@FXML
	public void showLogfileDirectory() {
		try {
			revealPathService.reveal(environment.getLogDir().orElseThrow());
		} catch (RevealFailedException e) {
			LOG.warn("Failed to reveal log files directory.", e);
		}
	}

	/* Helper classes */
	private static class NamedServiceConverter<T extends NamedServiceProvider> extends StringConverter<T> {

		@Override
		public String toString(T namedService) {
			if (namedService == null) {
				return null;
			} else {
				return namedService.getName();
			}
		}

		@Override
		public T fromString(String string) {
			throw new UnsupportedOperationException();
		}

	}

	private static class ServiceToSettingsConverter<T> extends StringConverter<T> {

		private final List<T> services;

		public ServiceToSettingsConverter(List<T> services) {
			this.services = services;
		}

		@Override
		public String toString(T service) {
			if (service == null) {
				return null;
			} else {
				return service.getClass().getName();
			}
		}

		@Override
		public T fromString(String string) {
			if (string == null) {
				return null;
			} else {
				return services.stream().filter(provider -> provider.getClass().getName().equals(string)).findAny().orElse(null);
			}
		}
	}

}
