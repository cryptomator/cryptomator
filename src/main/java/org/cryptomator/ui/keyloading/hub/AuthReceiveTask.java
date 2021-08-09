package org.cryptomator.ui.keyloading.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import java.net.URI;
import java.util.function.Consumer;

class AuthReceiveTask extends Task<EciesParams> {

	private static final Logger LOG = LoggerFactory.getLogger(AuthReceiveTask.class);

	private final Consumer<URI> redirectUriConsumer;

	/**
	 * Spawns a server and waits for the redirectUri to be called.
	 *
	 * @param redirectUriConsumer A callback invoked with the redirectUri, as soon as the server has started
	 */
	public AuthReceiveTask(Consumer<URI> redirectUriConsumer) {
		this.redirectUriConsumer = redirectUriConsumer;
	}

	@Override
	protected EciesParams call() throws Exception {
		try (var receiver = AuthReceiver.start()) {
			var redirectUri = receiver.getRedirectURL();
			Platform.runLater(() -> redirectUriConsumer.accept(redirectUri));
			LOG.debug("Waiting for key on {}", redirectUri);
			return receiver.receive();
		}
	}
}
