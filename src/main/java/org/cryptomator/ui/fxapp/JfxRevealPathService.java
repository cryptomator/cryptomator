package org.cryptomator.ui.fxapp;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;

import java.nio.file.Path;

/**
 * A {@link RevealPathService} service implementation using the JavaFX {@link javafx.application.HostServices#showDocument(String)} to reveal documents.
 * <p>
 * Internally the HostServices class uses GTK on Linux.
 *
 * @implNote {@link #reveal(Path)} only succeeds when the class {@link FxApplication} is initialized.
 */
@DisplayName("JavaFX HostServices (GTK)")
@OperatingSystem(OperatingSystem.Value.LINUX)
@Priority(10)
public class JfxRevealPathService implements RevealPathService {

	@Override
	public void reveal(Path p) throws RevealFailedException {
		var fxApp = FxApplication.INSTANCE.get();
		if (fxApp != null) {
			fxApp.getHostServices().showDocument(p.toUri().toString());
		} else {
			throw new RevealFailedException("JavaFX Application not initialized");
		}
	}

	@Override
	public boolean isSupported() {
		return true;
	}
}
