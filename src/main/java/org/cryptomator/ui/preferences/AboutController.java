package org.cryptomator.ui.preferences;

import com.google.common.io.CharStreams;
import org.cryptomator.common.Environment;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.updater.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneOffset;

@PreferencesScoped
public class AboutController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AboutController.class);

	private final String thirdPartyLicenseText;
	private final String fullApplicationVersion;
	private final int copyrightEndYear;

	@Inject
	AboutController(UpdateChecker updateChecker, Environment environment) {
		this.thirdPartyLicenseText = loadThirdPartyLicenseFile();
		StringBuilder sb = new StringBuilder(environment.getAppVersion());
		environment.getBuildNumber().ifPresent(s -> sb.append(" (").append(s).append(')'));
		this.fullApplicationVersion = sb.toString();
		// versions without timestamp (e.g. run from the IDE) are considered to be published just now
		this.copyrightEndYear = environment.getPublishTimestamp().orElseGet(Instant::now).atZone(ZoneOffset.UTC).getYear();
	}

	private static String loadThirdPartyLicenseFile() {
		try (InputStream in = AboutController.class.getResourceAsStream("/THIRD-PARTY.txt")) {
			return CharStreams.toString(new InputStreamReader(in));
		} catch (IOException | NullPointerException e) {
			LOG.error("Failed to load /THIRD-PARTY.txt", e);
			return "";
		}
	}

	/* Getter */

	public String getThirdPartyLicenseText() {
		return thirdPartyLicenseText;
	}

	public String getFullApplicationVersion() {
		return fullApplicationVersion;
	}

	public int getCopyrightEndYear() {
		return copyrightEndYear;
	}
}
