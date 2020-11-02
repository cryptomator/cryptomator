package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private final Binding<String> stackTrace;

	@Inject
	public VaultDetailUnknownErrorController(ObjectProperty<Vault> vault) {
		this.stackTrace = EasyBind.select(vault) //
				.selectObject(Vault::lastKnownExceptionProperty) //
				.map(this::provideStackTrace);
	}

	private String provideStackTrace(Throwable cause) {
		// TODO deduplicate ErrorModule.java
		if (cause != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			cause.printStackTrace(new PrintStream(baos));
			return baos.toString(StandardCharsets.UTF_8);
		} else {
			return "";
		}
	}

	/* Getter/Setter */

	public Binding<String> stackTraceProperty() {
		return stackTrace;
	}

	public String getStackTrace() {
		return stackTrace.getValue();
	}
}
