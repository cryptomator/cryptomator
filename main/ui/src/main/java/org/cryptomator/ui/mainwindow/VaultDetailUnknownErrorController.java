package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private final Binding<String> stackTrace;

	@Inject
	public VaultDetailUnknownErrorController(ObjectProperty<Vault> vault) {
		this.stackTrace = EasyBind.select(vault).selectObject(Vault::lastKnownExceptionProperty).map(this::provideStackTrace).orElse("");
	}

	private String provideStackTrace(Throwable cause) {
		// TODO deduplicate ErrorModule.java
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		cause.printStackTrace(new PrintStream(baos));
		return baos.toString(StandardCharsets.UTF_8);
	}

	/* Getter/Setter */

	public Binding<String> stackTraceProperty() {
		return stackTrace;
	}

	public String getStackTrace() {
		return stackTrace.getValue();
	}
}
