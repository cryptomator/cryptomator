package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@HealthCheckScoped
public class StartFailController implements FxController {

	private final Stage window;
	private final ObjectProperty<Throwable> loadError;
	private final ObjectProperty<FontAwesome5Icon> moreInfoIcon;

	/* FXML */
	public TitledPane moreInfoPane;

	@Inject
	public StartFailController(@HealthCheckWindow Stage window, HealthCheckComponent.LoadUnverifiedConfigResult configLoadResult) {
		Preconditions.checkNotNull(configLoadResult.error());
		this.window = window;
		this.loadError = new SimpleObjectProperty<>(configLoadResult.error());
		this.moreInfoIcon = new SimpleObjectProperty<>(FontAwesome5Icon.CARET_RIGHT);
	}

	public void initialize() {
		moreInfoPane.expandedProperty().addListener(this::setMoreInfoIcon);
	}

	private void setMoreInfoIcon(ObservableValue<? extends Boolean> observable, boolean wasExpanded, boolean willExpand) {
		moreInfoIcon.set(willExpand ? FontAwesome5Icon.CARET_DOWN : FontAwesome5Icon.CARET_RIGHT);
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter & Setter */

	public ObjectProperty<FontAwesome5Icon> moreInfoIconProperty() {
		return moreInfoIcon;
	}

	public FontAwesome5Icon getMoreInfoIcon() {
		return moreInfoIcon.getValue();
	}

	public String getStackTrace() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		loadError.get().printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	public String getLocalizedErrorMessage() {
		return loadError.get().getLocalizedMessage();
	}

	public boolean isParseException() {
		return loadError.get() instanceof VaultConfigLoadException;
	}

	public boolean isIoException() {
		return !isParseException();
	}

}
