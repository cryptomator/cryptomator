package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.KeychainBackend;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Stage window;
	private final Settings settings;
	private final boolean trayMenuSupported;
	private final Optional<AutoStartStrategy> autoStartStrategy;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final LicenseHolder licenseHolder;
	private final ExecutorService executor;
	private final ResourceBundle resourceBundle;
	private final Application application;
	private final Environment environment;
	private final Set<KeychainAccessProvider> keychainAccessProviders;
	private final ErrorComponent.Builder errorComponent;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public ChoiceBox<KeychainBackend> keychainBackendChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;

	@Inject
	GeneralPreferencesController(@PreferencesWindow Stage window, Settings settings, @Named("trayMenuSupported") boolean trayMenuSupported, Optional<AutoStartStrategy> autoStartStrategy, Set<KeychainAccessProvider> keychainAccessProviders, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, LicenseHolder licenseHolder, ExecutorService executor, ResourceBundle resourceBundle, Application application, Environment environment, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.settings = settings;
		this.trayMenuSupported = trayMenuSupported;
		this.autoStartStrategy = autoStartStrategy;
		this.keychainAccessProviders = keychainAccessProviders;
		this.selectedTabProperty = selectedTabProperty;
		this.licenseHolder = licenseHolder;
		this.executor = executor;
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

		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden());

		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode());

		autoStartStrategy.ifPresent(autoStart -> {
			autoStart.isAutoStartEnabled().thenAccept(enabled -> {
				Platform.runLater(() -> autoStartCheckbox.setSelected(enabled));
			});
		});

		nodeOrientationLtr.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.LEFT_TO_RIGHT);
		nodeOrientationRtl.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.RIGHT_TO_LEFT);
		nodeOrientation.selectedToggleProperty().addListener(this::toggleNodeOrientation);

		keychainBackendChoiceBox.getItems().addAll(getAvailableBackends());
		keychainBackendChoiceBox.setConverter(new KeychainBackendConverter(resourceBundle));
		keychainBackendChoiceBox.valueProperty().bindBidirectional(settings.keychainBackend());
	}

	private KeychainBackend[] getAvailableBackends() {
		var namesOfAvailableProviders = keychainAccessProviders.stream().map(KeychainAccessProvider::getClass).map(Class::getName).collect(Collectors.toUnmodifiableSet());
		return Arrays.stream(KeychainBackend.values()).filter(value -> namesOfAvailableProviders.contains(value.getProviderClass())).toArray(KeychainBackend[]::new);
	}

	public boolean isTrayMenuSupported() {
		return this.trayMenuSupported;
	}

	public boolean isAutoStartSupported() {
		return autoStartStrategy.isPresent();
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
		autoStartStrategy.ifPresent(autoStart -> {
			boolean enableAutoStart = autoStartCheckbox.isSelected();
			Task<Void> toggleTask = new ToggleAutoStartTask(autoStart, enableAutoStart);
			toggleTask.setOnFailed(event -> {
				autoStartCheckbox.setSelected(!enableAutoStart); // restore previous state
				LOG.error("Failed to toggle autostart.", event.getSource().getException());
				errorComponent.cause(event.getSource().getException()).window(window).returnToScene(window.getScene()).build().showErrorScene();
			});
			executor.execute(toggleTask);
		});
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}


	@FXML
	public void showDonationTab() {
		selectedTabProperty.set(SelectedPreferencesTab.DONATION_KEY);
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

	private static class KeychainBackendConverter extends StringConverter<KeychainBackend> {

		private final ResourceBundle resourceBundle;

		KeychainBackendConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(KeychainBackend impl) {
			return resourceBundle.getString("preferences.general.keychainBackend." + impl.getProviderClass());
		}

		@Override
		public KeychainBackend fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

	private static class ToggleAutoStartTask extends Task<Void> {

		private final AutoStartStrategy autoStart;
		private final boolean enable;

		public ToggleAutoStartTask(AutoStartStrategy autoStart, boolean enable) {
			this.autoStart = autoStart;
			this.enable = enable;

			setOnFailed(event -> LOG.error("Failed to toggle Autostart", getException()));
		}

		@Override
		protected Void call() throws Exception {
			if (enable) {
				autoStart.enableAutoStart();
			} else {
				autoStart.disableAutoStart();
			}
			return null;
		}
	}

}
