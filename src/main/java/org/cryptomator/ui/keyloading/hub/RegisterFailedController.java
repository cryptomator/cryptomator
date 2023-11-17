package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.concurrent.atomic.AtomicReference;

public class RegisterFailedController implements FxController {

	private final Stage window;
	private final Throwable registerException;
	private final SimpleBooleanProperty deviceAlreadyExisting;

	@Inject
	public RegisterFailedController(@KeyLoading Stage window, @Named("registerException") AtomicReference<Throwable> registerExceptionRef) {
		this.window = window;
		this.registerException = registerExceptionRef.get();
		this.deviceAlreadyExisting = new SimpleBooleanProperty(registerException instanceof DeviceAlreadyExistsException);
	}

	@FXML
	public void close() {
		window.close();
	}

	public boolean isDeviceAlreadyExisting() {
		return deviceAlreadyExisting.get();
	}

	public boolean isGenericError() {
		return !deviceAlreadyExisting.get();
	}

}
