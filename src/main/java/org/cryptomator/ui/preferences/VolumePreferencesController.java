package org.cryptomator.ui.preferences;

import org.cryptomator.common.mount.ActualMountService;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
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
	private final ObservableValue<ActualMountService> selectedMountService;
	private final BooleanExpression loopbackPortSupported;
	private final List<MountService> mountProviders;
	public ChoiceBox<MountService> volumeTypeChoiceBox;
	public TextField loopbackPortField;
	public Button loopbackPortApplyButton;

	@Inject
	VolumePreferencesController(Settings settings, List<MountService> mountProviders, ObservableValue<ActualMountService> actualMountService) {
		this.settings = settings;
		this.mountProviders = mountProviders;
		this.selectedMountService = actualMountService;
		this.loopbackPortSupported = BooleanExpression.booleanExpression(selectedMountService.map(as -> as.service().hasCapability(MountCapability.LOOPBACK_PORT)));
	}

	public void initialize() {
		volumeTypeChoiceBox.getItems().addAll(mountProviders);
		volumeTypeChoiceBox.setConverter(new MountServiceConverter());
		volumeTypeChoiceBox.getSelectionModel().select(selectedMountService.getValue().service());
		volumeTypeChoiceBox.valueProperty().addListener((observableValue, oldProvide, newProvider) -> settings.mountService().set(newProvider.getClass().getName()));

		loopbackPortField.setText(String.valueOf(settings.port().get()));
		loopbackPortApplyButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(loopbackPortField.textProperty()));
		loopbackPortApplyButton.disableProperty().bind(Bindings.createBooleanBinding(this::validateLoopbackPort, loopbackPortField.textProperty()).not());

	}

	private boolean validateLoopbackPort() {
		try {
			int port = Integer.parseInt(loopbackPortField.getText());
			return port == 0 // choose port automatically
					|| port >= Settings.MIN_PORT && port <= Settings.MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void doChangeLoopbackPort() {
		if (validateLoopbackPort()) {
			settings.port().set(Integer.parseInt(loopbackPortField.getText()));
		}
	}

	/* Property Getters */

	public BooleanExpression loopbackPortSupportedProperty() {
		return loopbackPortSupported;
	}

	public boolean isLoopbackPortSupported() {
		return loopbackPortSupported.get();
	}

	/* Helpers */

	private static class MountServiceConverter extends StringConverter<MountService> {

		@Override
		public String toString(MountService provider) {
			return provider== null? "Automatic" : provider.displayName(); //TODO: adjust message
		}

		@Override
		public MountService fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}


}
