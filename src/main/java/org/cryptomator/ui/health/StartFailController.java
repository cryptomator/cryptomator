package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

	@Inject
	public StartFailController(@HealthCheckWindow Stage window, HealthCheckComponent.LoadUnverifiedConfigResult configLoadResult) {
		Preconditions.checkNotNull(configLoadResult.error());
		this.window = window;
		this.loadError = new SimpleObjectProperty<>(configLoadResult.error());
		this.localizedErrorMessage = EasyBind.map(loadError, Throwable::getLocalizedMessage);
		this.parseException = Bindings.createBooleanBinding(() -> loadError.get() instanceof VaultConfigLoadException);
		this.ioException = parseException.not();
		this.stackTrace = EasyBind.map(loadError, this::createPrintableStacktrace);
		this.moreInfoIcon = new SimpleObjectProperty<>(FontAwesome5Icon.CARET_RIGHT);
	}

	public void initialize() {
		moreInfoPane.expandedProperty().addListener(this::setMoreInfoIcon);
	}

	private void setMoreInfoIcon(ObservableValue<? extends Boolean> observable, boolean wasExpanded, boolean willExpand) {
		moreInfoIcon.set(willExpand ? FontAwesome5Icon.CARET_DOWN : FontAwesome5Icon.CARET_RIGHT);
	}

	private String createPrintableStacktrace(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	@FXML
	public void close() {
		window.close();
	}

	public Binding<String> stackTraceProperty() {
		return stackTrace;
	}

	public String getStackTrace() {
		return stackTrace.getValue();
	}

	public Binding<String> localizedErrorMessageProperty() {
		return localizedErrorMessage;
	}

	public String getLocalizedErrorMessage() {
		return localizedErrorMessage.getValue();
	}

	public BooleanBinding parseExceptionProperty() {
		return parseException;
	}

	public boolean isParseException() {
		return parseException.getValue();
	}

	public BooleanBinding ioExceptionProperty() {
		return ioException;
	}

	public boolean isIoException() {
		return ioException.getValue();
	}

	public ObjectProperty<FontAwesome5Icon> moreInfoIconProperty() {
		return moreInfoIcon;
	}

	public FontAwesome5Icon getMoreInfoIcon() {
		return moreInfoIcon.getValue();
	}

	private final Stage window;
	private final ObjectProperty<Throwable> loadError;
	private final Binding<String> stackTrace;
	private final Binding<String> localizedErrorMessage;
	private final BooleanBinding ioException;
	private final BooleanBinding parseException;
	private final ObjectProperty<FontAwesome5Icon> moreInfoIcon;

	/* FXML */
	public TitledPane moreInfoPane;

}
