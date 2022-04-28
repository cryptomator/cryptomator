package org.cryptomator.ui.keyloading.hub;

import com.google.gson.JsonParser;
import io.github.coffeelibs.tinyoauth2client.AuthFlow;

import javafx.concurrent.Task;
import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

class AuthFlowTask extends Task<String> {

	private final HubConfig hubConfig;
	private final AuthFlowContext authFlowContext;
	private final Consumer<URI> redirectUriConsumer;

	/**
	 * Spawns a server and waits for the redirectUri to be called.
	 *
	 * @param hubConfig Configuration object holding parameters required by {@link AuthFlow}
	 * @param redirectUriConsumer A callback invoked with the redirectUri, as soon as the server has started
	 */
	public AuthFlowTask(HubConfig hubConfig, AuthFlowContext authFlowContext, Consumer<URI> redirectUriConsumer) {
		this.hubConfig = hubConfig;
		this.authFlowContext = authFlowContext;
		this.redirectUriConsumer = redirectUriConsumer;
	}

	@Override
	protected String call() throws IOException, InterruptedException {
		var response = AuthFlow.asClient(hubConfig.clientId) //
				.withSuccessRedirect(URI.create(hubConfig.authSuccessUrl + "&device=" + authFlowContext.deviceId())) //
				.withErrorRedirect(URI.create(hubConfig.authErrorUrl + "&device=" + authFlowContext.deviceId())) //
				.authorize(URI.create(hubConfig.authEndpoint), redirectUriConsumer) //
				.getAccessToken(URI.create(hubConfig.tokenEndpoint));
		var json = JsonParser.parseString(response);
		return json.getAsJsonObject().get("access_token").getAsString();
	}

}
