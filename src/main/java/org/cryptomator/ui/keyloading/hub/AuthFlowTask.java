package org.cryptomator.ui.keyloading.hub;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.net.URI;
import java.util.function.Consumer;

class AuthFlowTask extends Task<String> {

	private final Consumer<URI> redirectUriConsumer;

	/**
	 * Spawns a server and waits for the redirectUri to be called.
	 *
	 * @param hubConfig Configuration object holding parameters required by {@link AuthFlow}
	 * @param redirectUriConsumer A callback invoked with the redirectUri, as soon as the server has started
	 */
	public AuthFlowTask(HubConfig hubConfig, Consumer<URI> redirectUriConsumer) {
		this.hubConfig = hubConfig;
		this.redirectUriConsumer = redirectUriConsumer;
	}

	@Override
	protected String call() throws Exception {
		try (var authFlow = AuthFlow.init(hubConfig)) {
			return authFlow.run(uri -> Platform.runLater(() -> redirectUriConsumer.accept(uri)));
		}
	}

	private final HubConfig hubConfig;
}
