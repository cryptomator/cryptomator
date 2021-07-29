package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import java.net.URI;
import java.util.function.Consumer;

class ReceiveEncryptedMasterkeyTask extends Task<byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(ReceiveEncryptedMasterkeyTask.class);

	private final Consumer<URI> redirectUriConsumer;

	public ReceiveEncryptedMasterkeyTask(Consumer<URI> redirectUriConsumer) {
		this.redirectUriConsumer = redirectUriConsumer;
	}

	@Override
	protected byte[] call() throws Exception {
		try (var receiver = AuthReceiver.start()) {
			var redirectUri = receiver.getRedirectURL();
			LOG.debug("Waiting for key on {}", redirectUri);
			redirectUriConsumer.accept(redirectUri);
			var token = receiver.receive();
			return BaseEncoding.base64Url().decode(token);
		}
	}
}
