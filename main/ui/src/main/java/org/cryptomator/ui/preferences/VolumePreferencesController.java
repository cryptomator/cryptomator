package org.cryptomator.ui.preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.settings.WebDavUrlScheme;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

/**
 * TODO: if WebDAV is selected under Windows, show warning that specific mount options (like selecting a directory as mount point) are _not_ supported
 */
@PreferencesScoped
public class VolumePreferencesController implements FxController {

	private final Settings settings;
	private final BooleanBinding showWebDavSettings;
	private final BooleanBinding showWebDavScheme;
	public ChoiceBox<VolumeImpl> volumeTypeChoiceBox;
	public TextField webDavPortField;
	public Button changeWebDavPortButton;
	public ChoiceBox<WebDavUrlScheme> webDavUrlSchemeChoiceBox;

	@Inject
	VolumePreferencesController(Settings settings) {
		this.settings = settings;
		this.showWebDavSettings = Bindings.equal(settings.preferredVolumeImpl(), VolumeImpl.WEBDAV);
		this.showWebDavScheme = showWebDavSettings.and(new SimpleBooleanProperty(SystemUtils.IS_OS_LINUX)); //TODO: remove SystemUtils
	}

	public void initialize() {
		volumeTypeChoiceBox.getItems().addAll(Volume.getCurrentSupportedAdapters());
		//If the in the settings specified preferredVolumeImplementation isn't available, overwrite the settings to use the default VolumeImpl.WEBDAV
		if (!volumeTypeChoiceBox.getItems().contains(settings.preferredVolumeImpl().get())) {
			settings.preferredVolumeImpl().bind(new SimpleObjectProperty<>(VolumeImpl.WEBDAV));
		}
		volumeTypeChoiceBox.valueProperty().bindBidirectional(settings.preferredVolumeImpl());
		volumeTypeChoiceBox.setConverter(new VolumeImplConverter());

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

	private static class VolumeImplConverter extends StringConverter<VolumeImpl> {

		@Override
		public String toString(VolumeImpl impl) {
			return impl.getDisplayName();
		}

		@Override
		public VolumeImpl fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

}
