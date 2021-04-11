package org.cryptomator.ui.error;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GenericErrorController extends AbstractErrorController {

	private final String stackTrace;

	@Inject
	GenericErrorController(@ErrorReport Stage window, @ErrorReport @Nullable Scene previousScene, @Named("stackTrace") String stackTrace) {
		super(window, previousScene);
		this.stackTrace = stackTrace;
	}

	/* Getter/Setter */

	public String getStackTrace() {
		return this.stackTrace;
	}
}
