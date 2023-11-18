package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.autostart.ToggleAutoStartFailedException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
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
	private final Application application;
	private final Environment environment;
	private final List<KeychainAccessProvider> keychainAccessProviders;
	private final FxApplicationWindows appWindows;
	public CheckBox useKeychainCheckbox;
	public ChoiceBox<KeychainAccessProvider> keychainBackendChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox autoCloseVaultsCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;

	@Inject
	GeneralPreferencesController(@PreferencesWindow Stage window, Settings settings, Optional<AutoStartProvider> autoStartProvider, List<KeychainAccessProvider> keychainAccessProviders, Application application, Environment environment, FxApplicationWindows appWindows) {
		this.window = window;
		this.settings = settings;
		this.autoStartProvider = autoStartProvider;
		this.keychainAccessProviders = keychainAccessProviders;
		this.application = application;
		this.environment = environment;
		this.appWindows = appWindows;
	}

	// resolved unnecessary abstraction code smell using Inline Class technique
	@FXML
	public void initialize() {
		keychainBackendChoiceBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(KeychainAccessProvider provider) {
				return (provider != null) ? provider.getClass().getName() : null;
			}

			@Override
			public KeychainAccessProvider fromString(String string) {
				return (string != null) ?
						keychainAccessProviders.stream()
								.filter(provider -> provider.getClass().getName().equals(string))
								.findAny().orElse(null)
						: null;
			}
		});

		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden);
		autoCloseVaultsCheckbox.selectedProperty().bindBidirectional(settings.autoCloseVaults);
		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode);
		autoStartProvider.ifPresent(autoStart -> autoStartCheckbox.setSelected(autoStart.isEnabled()));

		keychainBackendChoiceBox.getItems().addAll(keychainAccessProviders);

		keychainBackendChoiceBox.setValue(
				keychainAccessProviders.stream()
						.filter(provider -> provider.getClass().getName().equals(settings.keychainProvider.get()))
						.findAny().orElse(null)
		);

		keychainBackendChoiceBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(KeychainAccessProvider provider) {
				return (provider != null) ? provider.displayName() : null;
			}

			@Override
			public KeychainAccessProvider fromString(String string) {
				return (string != null) ?
						keychainAccessProviders.stream()
								.filter(provider -> provider.displayName().equals(string))
								.findAny().orElse(null)
						: null;
			}
		});

		Bindings.bindBidirectional(settings.keychainProvider, keychainBackendChoiceBox.valueProperty(), new StringConverter<>() {
			@Override
			public String toString(KeychainAccessProvider provider) {
				return (provider != null) ? provider.getClass().getName() : null;
			}

			@Override
			public KeychainAccessProvider fromString(String string) {
				return (string != null) ?
						keychainAccessProviders.stream()
								.filter(provider -> provider.getClass().getName().equals(string))
								.findAny().orElse(null)
						: null;
			}
		});

		useKeychainCheckbox.selectedProperty().bindBidirectional(settings.useKeychain);
		keychainBackendChoiceBox.disableProperty().bind(useKeychainCheckbox.selectedProperty().not());
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

//	private static class KeychainProviderClassNameConverter extends StringConverter<KeychainAccessProvider> {
//
//		private final List<KeychainAccessProvider> keychainAccessProviders;
//
//		public KeychainProviderClassNameConverter(List<KeychainAccessProvider> keychainAccessProviders) {
//			this.keychainAccessProviders = keychainAccessProviders;
//		}
//
//		@Override
//		public String toString(KeychainAccessProvider provider) {
//			if (provider == null) {
//				return null;
//			} else {
//				return provider.getClass().getName();
//			}
//		}
//
//		@Override
//		public KeychainAccessProvider fromString(String string) {
//			if (string == null) {
//				return null;
//			} else {
//				return keychainAccessProviders.stream().filter(provider -> provider.getClass().getName().equals(string)).findAny().orElse(null);
//			}
//		}
//	}
}
