package org.cryptomator.ui.preferences;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.settings.WebDavUrlScheme;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.util.List;

/**
 * TODO: if WebDAV is selected under Windows, show warning that specific mount options (like selecting a directory as mount point) are _not_ supported
 */
@PreferencesScoped
public class VolumePreferencesController implements FxController {

	private final Settings settings;
	private final BooleanBinding showWebDavSettings;
	private final ObservableValue<MountService> selectedMountService;
	private final BooleanBinding showWebDavScheme;
	private final List<MountService> mountProviders;
	public ChoiceBox<MountService> volumeTypeChoiceBox;
	public TextField webDavPortField;
	public Button changeWebDavPortButton;
	public ChoiceBox<WebDavUrlScheme> webDavUrlSchemeChoiceBox;

	@Inject
	VolumePreferencesController(Settings settings, List<MountService> mountProviders, ObservableValue<MountService> selectedMountService) {
		this.settings = settings;
		this.mountProviders = mountProviders;
		this.showWebDavSettings = Bindings.equal(settings.preferredVolumeImpl(), VolumeImpl.WEBDAV);
		this.selectedMountService = selectedMountService;
		this.showWebDavScheme = showWebDavSettings.and(new SimpleBooleanProperty(SystemUtils.IS_OS_LINUX)); //TODO: remove SystemUtils
	}

	public void initialize() {
		volumeTypeChoiceBox.getItems().addAll(mountProviders);
		volumeTypeChoiceBox.setConverter(new MountServiceConverter());
		volumeTypeChoiceBox.getSelectionModel().select(selectedMountService.getValue());
		volumeTypeChoiceBox.valueProperty().addListener((observableValue, oldProvide, newProvider) -> settings.mountService().set(newProvider.getClass().getName()));

		webDavPortField.setText(String.valueOf(settings.port().get()));
		changeWebDavPortButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(webDavPortField.textProperty()));
		changeWebDavPortButton.disableProperty().bind(Bindings.createBooleanBinding(this::validateWebDavPort, webDavPortField.textProperty()).not());

		webDavUrlSchemeChoiceBox.getItems().addAll(WebDavUrlScheme.values());
		webDavUrlSchemeChoiceBox.valueProperty().bindBidirectional(settings.preferredGvfsScheme());
		webDavUrlSchemeChoiceBox.setConverter(new WebDavUrlSchemeConverter());
	}

	private boolean validateWebDavPort() {
		try {
			int port = Integer.parseInt(webDavPortField.getText());
			return port == 0 // choose port automatically
					|| port >= Settings.MIN_PORT && port <= Settings.MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void doChangeWebDavPort() {
		settings.port().set(Integer.parseInt(webDavPortField.getText()));
	}

	/* Property Getters */

	public BooleanBinding showWebDavSettingsProperty() {
		return showWebDavSettings;
	}

	public Boolean getShowWebDavSettings() {
		return showWebDavSettings.get();
	}

	public BooleanBinding showWebDavSchemeProperty() {
		return showWebDavScheme;
	}

	public Boolean getShowWebDavScheme() {
		return showWebDavScheme.get();
	}

	/* Helper classes */

	private static class WebDavUrlSchemeConverter extends StringConverter<WebDavUrlScheme> {

		@Override
		public String toString(WebDavUrlScheme scheme) {
			return scheme.getDisplayName();
		}

		@Override
		public WebDavUrlScheme fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

	private static class MountServiceConverter extends StringConverter<MountService> {

		@Override
		public String toString(MountService provider) {
			return provider== null? "None" : provider.displayName(); //TODO: adjust message
		}

		@Override
		public MountService fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

}
