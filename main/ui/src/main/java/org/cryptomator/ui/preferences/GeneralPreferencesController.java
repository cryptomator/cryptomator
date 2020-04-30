package org.cryptomator.ui.preferences;

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
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
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
	private final ResourceBundle resourceBundle;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;
	public CheckBox autoStartCheckbox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;

	@Inject
	GeneralPreferencesController(Settings settings, @Named("trayMenuSupported") boolean trayMenuSupported, Optional<AutoStartStrategy> autoStartStrategy, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, LicenseHolder licenseHolder, ExecutorService executor, ResourceBundle resourceBundle) {
		this.settings = settings;
		this.trayMenuSupported = trayMenuSupported;
		this.autoStartStrategy = autoStartStrategy;
		this.selectedTabProperty = selectedTabProperty;
		this.licenseHolder = licenseHolder;
		this.executor = executor;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.values());
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

}
