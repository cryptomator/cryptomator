package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.autostart.ToggleAutoStartFailedException;
import org.cryptomator.integrations.common.NamedServiceProvider;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.integrations.quickaccess.QuickAccessService;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.List;
import java.util.Optional;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Stage window;
	private final Settings settings;
	private final Optional<AutoStartProvider> autoStartProvider;
	private final List<QuickAccessService> quickAccessServices;
	private final Application application;
	private final Environment environment;
	private final List<KeychainAccessProvider> keychainAccessProviders;
	private final FxApplicationWindows appWindows;
	public CheckBox useKeychainCheckbox;
	public ChoiceBox<KeychainAccessProvider> keychainBackendChoiceBox;
	public CheckBox useQuickAccessCheckbox;
	public ChoiceBox<QuickAccessService> quickAccessServiceChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox autoCloseVaultsCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;

	@Inject
	GeneralPreferencesController(@PreferencesWindow Stage window, Settings settings, Optional<AutoStartProvider> autoStartProvider, List<KeychainAccessProvider> keychainAccessProviders, List<QuickAccessService> quickAccessServices, Application application, Environment environment, FxApplicationWindows appWindows) {
		this.window = window;
		this.settings = settings;
		this.autoStartProvider = autoStartProvider;
		this.keychainAccessProviders = keychainAccessProviders;
		this.quickAccessServices = quickAccessServices;
		this.application = application;
		this.environment = environment;
		this.appWindows = appWindows;
	}

	@FXML
	public void initialize() {
		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden);
		autoCloseVaultsCheckbox.selectedProperty().bindBidirectional(settings.autoCloseVaults);
		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode);
		autoStartProvider.ifPresent(autoStart -> autoStartCheckbox.setSelected(autoStart.isEnabled()));

		var keychainSettingsConverter = new ServiceToSettingsConverter<>(keychainAccessProviders);
		keychainBackendChoiceBox.getItems().addAll(keychainAccessProviders);
		keychainBackendChoiceBox.setValue(keychainSettingsConverter.fromString(settings.keychainProvider.get()));
		keychainBackendChoiceBox.setConverter(new KeychainProviderDisplayNameConverter());
		Bindings.bindBidirectional(settings.keychainProvider, keychainBackendChoiceBox.valueProperty(), keychainSettingsConverter);
		useKeychainCheckbox.selectedProperty().bindBidirectional(settings.useKeychain);
		keychainBackendChoiceBox.disableProperty().bind(useKeychainCheckbox.selectedProperty().not());

		useQuickAccessCheckbox.selectedProperty().bindBidirectional(settings.useQuickAccess);
		var quickAccessSettingsConverter = new ServiceToSettingsConverter<>(quickAccessServices);
		quickAccessServiceChoiceBox.getItems().addAll(quickAccessServices);
		quickAccessServiceChoiceBox.setValue(quickAccessSettingsConverter.fromString(settings.quickAccessService.get()));
		quickAccessServiceChoiceBox.setConverter(new NamedServiceConverter<>());
		Bindings.bindBidirectional(settings.quickAccessService, quickAccessServiceChoiceBox.valueProperty(), quickAccessSettingsConverter);
		quickAccessServiceChoiceBox.disableProperty().bind(useQuickAccessCheckbox.selectedProperty().not());
	}

	public boolean isAutoStartSupported() {
		return autoStartProvider.isPresent();
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
	public void showLogfileDirectory() {
		environment.getLogDir().ifPresent(logDirPath -> application.getHostServices().showDocument(logDirPath.toUri().toString()));
	}

	/* Helper classes */

	private static class KeychainProviderDisplayNameConverter extends StringConverter<KeychainAccessProvider> {

		@Override
		public String toString(KeychainAccessProvider provider) {
			if (provider == null) {
				return null;
			} else {
				return provider.displayName();
			}
		}

		@Override
		public KeychainAccessProvider fromString(String string) {
			throw new UnsupportedOperationException();
		}

	}

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
