package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.autostart.ToggleAutoStartFailedException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Stage window;
	private final Settings settings;
	private final boolean trayMenuInitialized;
	private final boolean trayMenuSupported;
	private final Optional<AutoStartProvider> autoStartProvider;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final LicenseHolder licenseHolder;
	private final ResourceBundle resourceBundle;
	private final Application application;
	private final Environment environment;
	private final Set<KeychainAccessProvider> keychainAccessProviders;
	private final ErrorComponent.Builder errorComponent;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public ChoiceBox<KeychainAccessProvider> keychainBackendChoiceBox;
	public CheckBox showMinimizeButtonCheckbox;
	public CheckBox showTrayIconCheckbox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;


	@Inject
	GeneralPreferencesController(@PreferencesWindow Stage window, Settings settings, TrayMenuComponent trayMenu, Optional<AutoStartProvider> autoStartProvider, Set<KeychainAccessProvider> keychainAccessProviders, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, LicenseHolder licenseHolder, ResourceBundle resourceBundle, Application application, Environment environment, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.settings = settings;
		this.trayMenuInitialized = trayMenu.isInitialized();
		this.trayMenuSupported = trayMenu.isSupported();
		this.autoStartProvider = autoStartProvider;
		this.keychainAccessProviders = keychainAccessProviders;
		this.selectedTabProperty = selectedTabProperty;
		this.licenseHolder = licenseHolder;
		this.resourceBundle = resourceBundle;
		this.application = application;
		this.environment = environment;
		this.errorComponent = errorComponent;
	}

	@FXML
	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.applicableValues());
		if (!themeChoiceBox.getItems().contains(settings.theme().get())) {
			settings.theme().set(UiTheme.LIGHT);
		}
		themeChoiceBox.valueProperty().bindBidirectional(settings.theme());
		themeChoiceBox.setConverter(new UiThemeConverter(resourceBundle));

		showMinimizeButtonCheckbox.selectedProperty().bindBidirectional(settings.showMinimizeButton());

		showTrayIconCheckbox.selectedProperty().bindBidirectional(settings.showTrayIcon());

		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden());

		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode());

		autoStartProvider.ifPresent(autoStart -> autoStartCheckbox.setSelected(autoStart.isEnabled()));

		nodeOrientationLtr.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.LEFT_TO_RIGHT);
		nodeOrientationRtl.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.RIGHT_TO_LEFT);
		nodeOrientation.selectedToggleProperty().addListener(this::toggleNodeOrientation);

		var keychainSettingsConverter = new KeychainProviderClassNameConverter(keychainAccessProviders);
		keychainBackendChoiceBox.getItems().addAll(keychainAccessProviders);
		keychainBackendChoiceBox.setValue(keychainSettingsConverter.fromString(settings.keychainProvider().get()));
		keychainBackendChoiceBox.setConverter(new KeychainProviderDisplayNameConverter());
		Bindings.bindBidirectional(settings.keychainProvider(), keychainBackendChoiceBox.valueProperty(), keychainSettingsConverter);
	}


	public boolean isTrayMenuInitialized() {
		return trayMenuInitialized;
	}

	public boolean isTrayMenuSupported() {
		return trayMenuSupported;
	}

	public boolean isAutoStartSupported() {
		return autoStartProvider.isPresent();
	}

	private void toggleNodeOrientation(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		if (nodeOrientationLtr.equals(newValue)) {
			settings.userInterfaceOrientation().set(NodeOrientation.LEFT_TO_RIGHT);
		} else if (nodeOrientationRtl.equals(newValue)) {
			settings.userInterfaceOrientation().set(NodeOrientation.RIGHT_TO_LEFT);
		} else {
			LOG.warn("Unexpected toggle option {}", newValue);
		}
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
				errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
			}
		});
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}


	@FXML
	public void showContributeTab() {
		selectedTabProperty.set(SelectedPreferencesTab.CONTRIBUTE);
	}

	@FXML
	public void showLogfileDirectory() {
		environment.getLogDir().ifPresent(logDirPath -> application.getHostServices().showDocument(logDirPath.toUri().toString()));
	}

	/* Helper classes */

	private static class UiThemeConverter extends StringConverter<UiTheme> {

		private final ResourceBundle resourceBundle;

		UiThemeConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(UiTheme impl) {
			return resourceBundle.getString(impl.getDisplayName());
		}

		@Override
		public UiTheme fromString(String string) {
			throw new UnsupportedOperationException();
		}

	}

	private class KeychainProviderDisplayNameConverter extends StringConverter<KeychainAccessProvider> {

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

	private static class KeychainProviderClassNameConverter extends StringConverter<KeychainAccessProvider> {

		private final Set<KeychainAccessProvider> keychainAccessProviders;

		public KeychainProviderClassNameConverter(Set<KeychainAccessProvider> keychainAccessProviders) {
			this.keychainAccessProviders = keychainAccessProviders;
		}

		@Override
		public String toString(KeychainAccessProvider provider) {
			if (provider == null) {
				return null;
			} else {
				return provider.getClass().getName();
			}
		}

		@Override
		public KeychainAccessProvider fromString(String string) {
			if (string == null) {
				return null;
			} else {
				return keychainAccessProviders.stream().filter(provider -> provider.getClass().getName().equals(string)).findAny().orElse(null);
			}
		}
	}
}
