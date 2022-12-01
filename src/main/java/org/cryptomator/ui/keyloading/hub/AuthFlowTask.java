package org.cryptomator.ui.keyloading.hub;

import com.google.gson.JsonParser;
import io.github.coffeelibs.tinyoauth2client.AuthFlow;
import io.github.coffeelibs.tinyoauth2client.TinyOAuth2;
import io.github.coffeelibs.tinyoauth2client.http.response.Response;

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
		var response = TinyOAuth2.client(hubConfig.clientId) //
				.withTokenEndpoint(URI.create(hubConfig.tokenEndpoint)) //
				.authFlow(URI.create(hubConfig.authEndpoint)) //
				.setSuccessResponse(Response.redirect(URI.create(hubConfig.authSuccessUrl + "&device=" + authFlowContext.deviceId()))) //
				.setErrorResponse(Response.redirect(URI.create(hubConfig.authErrorUrl + "&device=" + authFlowContext.deviceId()))) //
				.authorize(redirectUriConsumer);
		if (response.statusCode() != 200) {
			throw new NotOkResponseException("Authorization returned status code " + response.statusCode());
		}
		var json = JsonParser.parseString(response.body());
		return json.getAsJsonObject().get("access_token").getAsString();
	}

	public static class NotOkResponseException extends RuntimeException {

		NotOkResponseException(String msg) {
			super(msg);
		}
	}

}
