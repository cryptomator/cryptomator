package org.cryptomator.ui.keyloading.hub;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.net.URI;
import java.util.function.Consumer;

class AuthFlowTask extends Task<String> {

	private final URI authUri;
	private final URI tokenUri;
	private final String clientId;
	private final Consumer<URI> redirectUriConsumer;

	/**
	 * Spawns a server and waits for the redirectUri to be called.
	 *
	 * @param redirectUriConsumer A callback invoked with the redirectUri, as soon as the server has started
	 */
	public AuthFlowTask(URI authUri, URI tokenUri, String clientId, Consumer<URI> redirectUriConsumer) {
		this.authUri = authUri;
		this.tokenUri = tokenUri;
		this.clientId = clientId;
		this.redirectUriConsumer = redirectUriConsumer;
	}

	@Override
	protected String call() throws Exception {
		try (var authFlow = AuthFlow.init(authUri, tokenUri, clientId)) {
			return authFlow.run(uri -> Platform.runLater(() -> redirectUriConsumer.accept(uri)));
		}
	}
}
