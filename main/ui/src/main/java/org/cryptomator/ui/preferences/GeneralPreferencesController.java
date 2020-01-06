package org.cryptomator.ui.preferences;

import javafx.beans.value.ObservableValue;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {
	
	private static final Logger LOG = LoggerFactory.getLogger(GeneralPreferencesController.class);

	private final Settings settings;
	private final boolean trayMenuSupported;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;
	public ToggleGroup nodeOrientation;
	public RadioButton nodeOrientationLtr;
	public RadioButton nodeOrientationRtl;

	@Inject
	GeneralPreferencesController(Settings settings, @Named("trayMenuSupported") boolean trayMenuSupported) {
		this.settings = settings;
		this.trayMenuSupported = trayMenuSupported;
	}

	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.values());
		themeChoiceBox.valueProperty().bindBidirectional(settings.theme());
		themeChoiceBox.setConverter(new UiThemeConverter());

		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden());

		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode());

		nodeOrientationLtr.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.LEFT_TO_RIGHT);
		nodeOrientationRtl.setSelected(settings.userInterfaceOrientation().get() == NodeOrientation.RIGHT_TO_LEFT);
		nodeOrientation.selectedToggleProperty().addListener(this::toggleNodeOrientation);
	}

	public boolean isTrayMenuSupported() {
		return this.trayMenuSupported;
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

	/* Helper classes */

	private static class UiThemeConverter extends StringConverter<UiTheme> {

		@Override
		public String toString(UiTheme impl) {
			return impl.getDisplayName();
		}

		@Override
		public UiTheme fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

}
