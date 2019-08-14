package org.cryptomator.ui.preferences;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@PreferencesScoped
public class GeneralPreferencesController implements FxController {

	private final Settings settings;
	public ChoiceBox<UiTheme> themeChoiceBox;
	public CheckBox startHiddenCheckbox;
	public CheckBox debugModeCheckbox;

	@Inject
	GeneralPreferencesController(Settings settings) {
		this.settings = settings;
	}

	public void initialize() {
		themeChoiceBox.getItems().addAll(UiTheme.values());
		themeChoiceBox.valueProperty().bindBidirectional(settings.theme());
		themeChoiceBox.setConverter(new UiThemeConverter());

		startHiddenCheckbox.selectedProperty().bindBidirectional(settings.startHidden());

		debugModeCheckbox.selectedProperty().bindBidirectional(settings.debugMode());
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
