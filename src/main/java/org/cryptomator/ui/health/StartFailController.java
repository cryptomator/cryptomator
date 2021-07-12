package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@HealthCheckScoped
public class StartFailController implements FxController {

	@Inject
	public StartFailController(@HealthCheckWindow Stage window, HealthCheckComponent.LoadUnverifiedConfigResult configLoadResult) {
		Preconditions.checkNotNull(configLoadResult.error());
		this.window = window;
		this.loadError = new SimpleObjectProperty<>(configLoadResult.error());
		this.localizedErrorMessage = new SimpleStringProperty(configLoadResult.error().getLocalizedMessage());
		this.typeParseException = Bindings.createBooleanBinding(() -> loadError.get() instanceof VaultConfigLoadException);
		this.typeIOException = typeParseException.not();

	}

	@FXML
	public void close() {
		window.close();
	}

	private final Stage window;
	private final ObjectProperty<Throwable> loadError;
	private final StringProperty localizedErrorMessage;
	private final BooleanBinding typeIOException;
	private final BooleanBinding typeParseException;

}
