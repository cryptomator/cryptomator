package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.common.Environment;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.nio.file.Files;

@KeyLoadingScoped
public class P12Controller implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(P12Controller.class);

	private final Stage window;
	private final Environment env;
	private final UserInteractionLock<HubKeyLoadingModule.AuthFlow> userInteraction;

	@Inject
	public P12Controller(@KeyLoading Stage window, Environment env, UserInteractionLock<HubKeyLoadingModule.AuthFlow> userInteraction) {
		this.window = window;
		this.env = env;
		this.userInteraction = userInteraction;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (userInteraction.awaitingInteraction().get()) {
			LOG.debug("P12 loading cancelled by user.");
			userInteraction.interacted(HubKeyLoadingModule.AuthFlow.CANCELLED);
		}
	}

	/* Getter/Setter */

	public boolean isP12Present() {
		return env.getP12Path().anyMatch(Files::isRegularFile);
	}

}
