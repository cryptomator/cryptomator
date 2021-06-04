package org.cryptomator.ui.migration;

import dagger.Lazy;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@MigrationScoped
public class MigrationCapabilityErrorController implements FxController {

	private final Stage window;
	private final ResourceBundle localization;
	private final Lazy<Scene> startScene;
	private final StringBinding missingCapabilityDescription;
	private final ReadOnlyObjectProperty<FileSystemCapabilityChecker.Capability> missingCapability;

	@Inject
	MigrationCapabilityErrorController(@MigrationWindow Stage window, @Named("capabilityErrorCause") ObjectProperty<FileSystemCapabilityChecker.Capability> missingCapability, ResourceBundle localization, @FxmlScene(FxmlFile.MIGRATION_START) Lazy<Scene> startScene) {
		this.window = window;
		this.missingCapability = missingCapability;
		this.localization = localization;
		this.startScene = startScene;
		this.missingCapabilityDescription = Bindings.createStringBinding(this::getMissingCapabilityDescription, missingCapability);
	}

	@FXML
	public void back() {
		window.setScene(startScene.get());
	}

	/* Getters */

	public StringBinding missingCapabilityDescriptionProperty() {
		return missingCapabilityDescription;
	}

	public String getMissingCapabilityDescription() {
		FileSystemCapabilityChecker.Capability c = missingCapability.get();
		if (c != null) {
			return localization.getString("migration.error.missingFileSystemCapabilities.reason." + c.name());
		} else {
			return null;
		}
	}
}
