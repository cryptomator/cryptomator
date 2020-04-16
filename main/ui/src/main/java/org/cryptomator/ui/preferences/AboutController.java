package org.cryptomator.ui.preferences;

import com.google.common.io.CharStreams;
import org.cryptomator.common.Environment;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@PreferencesScoped
public class AboutController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AboutController.class);

	private final String thirdPartyLicenseText;
	private final String applicationVersion;

	@Inject
	AboutController(UpdateChecker updateChecker, Environment environment) {
		this.thirdPartyLicenseText = loadThirdPartyLicenseFile();
		StringBuilder sb = new StringBuilder(updateChecker.currentVersionProperty().get());
		environment.getBuildNumber().ifPresent(s -> sb.append(" (").append(s).append(')'));
		this.applicationVersion = sb.toString();
	}

	private static String loadThirdPartyLicenseFile() {
		try (InputStream in = AboutController.class.getResourceAsStream("/license/THIRD-PARTY.txt")) {
			return CharStreams.toString(new InputStreamReader(in));
		} catch (IOException | NullPointerException e) {
			LOG.error("Failed to load /license/THIRD-PARTY.txt", e);
			return "";
		}
	}

	/* Getter */

	public String getThirdPartyLicenseText() {
		return thirdPartyLicenseText;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}
}
