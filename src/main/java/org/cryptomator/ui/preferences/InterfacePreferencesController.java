package org.cryptomator.ui.preferences;

import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.launcher.SupportedLanguages;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import java.util.Locale;
import java.util.ResourceBundle;

@PreferencesScoped
public class InterfacePreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(InterfacePreferencesController.class);

	private final Settings settings;
	private final boolean trayMenuInitialized;
	private final boolean trayMenuSupported;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final LicenseHolder licenseHolder;
	private final ResourceBundle resourceBundle;
	private final SupportedLanguages supportedLanguages;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public CheckBox showTrayIconCheckbox;
	public ChoiceBox<String> preferredLanguageChoiceBox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;

	@Inject
	InterfacePreferencesController(Settings settings, SupportedLanguages supportedLanguages, TrayMenuComponent trayMenu, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, LicenseHolder licenseHolder, ResourceBundle resourceBundle) {
		this.settings = settings;
		this.trayMenuInitialized = trayMenu.isInitialized();
		this.trayMenuSupported = trayMenu.isSupported();
		this.selectedTabProperty = selectedTabProperty;
		this.licenseHolder = licenseHolder;
		this.resourceBundle = resourceBundle;
		this.supportedLanguages = supportedLanguages;
	}

	@FXML
	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.applicableValues());
		if (!themeChoiceBox.getItems().contains(settings.theme.get())) {
			settings.theme.set(UiTheme.LIGHT);
		}
		themeChoiceBox.valueProperty().bindBidirectional(settings.theme);
		themeChoiceBox.setConverter(new UiThemeConverter(resourceBundle));

		showTrayIconCheckbox.selectedProperty().bindBidirectional(settings.showTrayIcon);

		preferredLanguageChoiceBox.getItems().addAll(supportedLanguages.getLanguageTags());
		preferredLanguageChoiceBox.valueProperty().bindBidirectional(settings.language);
		preferredLanguageChoiceBox.setConverter(new LanguageTagConverter(resourceBundle));

		nodeOrientationLtr.setSelected(settings.userInterfaceOrientation.get() == NodeOrientation.LEFT_TO_RIGHT);
		nodeOrientationRtl.setSelected(settings.userInterfaceOrientation.get() == NodeOrientation.RIGHT_TO_LEFT);
		nodeOrientation.selectedToggleProperty().addListener(this::toggleNodeOrientation);
	}


	public boolean isTrayMenuInitialized() {
		return trayMenuInitialized;
	}

	public boolean isTrayMenuSupported() {
		return trayMenuSupported;
	}

	private void toggleNodeOrientation(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		if (nodeOrientationLtr.equals(newValue)) {
			settings.userInterfaceOrientation.set(NodeOrientation.LEFT_TO_RIGHT);
		} else if (nodeOrientationRtl.equals(newValue)) {
			settings.userInterfaceOrientation.set(NodeOrientation.RIGHT_TO_LEFT);
		} else {
			LOG.warn("Unexpected toggle option {}", newValue);
		}
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}


	@FXML
	public void showContributeTab() {
		selectedTabProperty.set(SelectedPreferencesTab.CONTRIBUTE);
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

	private static class LanguageTagConverter extends StringConverter<String> {

		private final ResourceBundle resourceBundle;

		LanguageTagConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(String tag) {
			if (tag == null) {
				return resourceBundle.getString("preferences.interface.language.auto");
			} else {
				var locale = Locale.forLanguageTag(tag);
				return locale.getDisplayName();
			}
		}

		@Override
		public String fromString(String displayLanguage) {
			throw new UnsupportedOperationException();
		}
	}

}
