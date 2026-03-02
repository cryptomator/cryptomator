package org.cryptomator.ui.fxapp;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;

import java.nio.file.Path;

@DisplayName("JavaFX Host Service")
@OperatingSystem(OperatingSystem.Value.LINUX)
@Priority(1)
public class JfxRevealPathService implements RevealPathService {

	@Override
	public void reveal(Path p) throws RevealFailedException {
		var fxApp = FxApplication.FX_APP_REF.get();
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
