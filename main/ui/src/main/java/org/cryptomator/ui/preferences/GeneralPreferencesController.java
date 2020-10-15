package org.cryptomator.ui.preferences;

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
import javafx.util.StringConverter;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.KeychainBackend;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.keychain.KeychainAccessStrategy;
import org.cryptomator.keychain.LinuxSystemKeychainAccess;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Settings settings;
	private final boolean trayMenuSupported;
	private final Optional<AutoStartStrategy> autoStartStrategy;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final LicenseHolder licenseHolder;
	private final ExecutorService executor;
	private final ResourceBundle resourceBundleUiTheme;
	private final ResourceBundle resourceBundleKcBackend;
	private final Application application;
	private final Environment environment;
	private Optional<KeychainAccessStrategy> keychain;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public ChoiceBox<KeychainBackend> keychainBackendChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;

	@Inject
	GeneralPreferencesController(Settings settings, @Named("trayMenuSupported") boolean trayMenuSupported, Optional<AutoStartStrategy> autoStartStrategy, Optional<KeychainAccessStrategy> keychain, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, LicenseHolder licenseHolder, ExecutorService executor, ResourceBundle resourceBundleUiTheme, ResourceBundle resourceBundleKcBackend, Application application, Environment environment) {
		this.settings = settings;
		this.trayMenuSupported = trayMenuSupported;
		this.autoStartStrategy = autoStartStrategy;
		this.keychain = keychain;
		this.selectedTabProperty = selectedTabProperty;
		this.licenseHolder = licenseHolder;
		this.executor = executor;
		this.resourceBundleUiTheme = resourceBundleUiTheme;
		this.resourceBundleKcBackend = resourceBundleKcBackend;
		this.application = application;
		this.environment = environment;
	}

	@FXML
	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.applicableValues());
		if (!themeChoiceBox.getItems().contains(settings.theme().get())) {
			settings.theme().set(UiTheme.LIGHT);
		}
		themeChoiceBox.valueProperty().bindBidirectional(settings.theme());
		themeChoiceBox.setConverter(new UiThemeConverter(resourceBundleUiTheme));

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
		keychainBackendChoiceBox.setConverter(new KeychainBackendConverter(resourceBundleKcBackend));
		keychainBackendChoiceBox.valueProperty().bindBidirectional(settings.keychainBackend());
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
			toggleTask.setOnFailed(evt -> autoStartCheckbox.setSelected(!enableAutoStart)); // restore previous state
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
			return resourceBundle.getString(impl.getDisplayName());
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

	private KeychainBackend[] getAvailableBackends() {
		if (!keychain.isPresent()) {
			return new KeychainBackend[]{};
		}
		if (SystemUtils.IS_OS_LINUX) {
			EnumSet<KeychainBackend> backends = LinuxSystemKeychainAccess.getAvailableKeychainBackends();
			keychainBackendChoiceBox.setValue(LinuxSystemKeychainAccess.getBackendActivated());
			return backends.size() > 0 ? backends.toArray(KeychainBackend[]::new) : new KeychainBackend[]{};
		}
		if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS) {
			keychainBackendChoiceBox.setValue(Arrays.stream(KeychainBackend.supportedBackends()).findFirst().orElseThrow(IllegalStateException::new));
			return KeychainBackend.supportedBackends();
		}
		return new KeychainBackend[]{};
	}
}
