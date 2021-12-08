package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import com.google.common.escape.Escaper;
import com.google.common.io.BaseEncoding;
import com.google.common.net.PercentEscaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple OAuth 2.0 Authentication Code Flow with {@link PKCE}.
 * <p>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8252">RFC 8252</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749">RFC 6749</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7636">RFC 7636</a>
 */
class AuthFlow implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(AuthFlow.class);
	private static final SecureRandom CSPRNG = new SecureRandom();
	private static final BaseEncoding BASE64URL = BaseEncoding.base64Url().omitPadding();
	public static final Escaper QUERY_STRING_ESCAPER = new PercentEscaper("-_.!~*'()@:$,;/?", false);

	private final AuthFlowReceiver receiver;
	private final URI authEndpoint; // see https://datatracker.ietf.org/doc/html/rfc6749#section-3.1
	private final URI tokenEndpoint; // see https://datatracker.ietf.org/doc/html/rfc6749#section-3.2
	private final String clientId; // see https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.1

	private AuthFlow(AuthFlowReceiver receiver, HubConfig hubConfig) {
		this.receiver = receiver;
		this.authEndpoint = URI.create(hubConfig.authEndpoint);
		this.tokenEndpoint = URI.create(hubConfig.tokenEndpoint);
		this.clientId = hubConfig.clientId;
	}

	/**
	 * Prepares an Authorization Code Flow with PKCE.
	 * <p>
	 * This will start a loopback server, so make sure to {@link #close()} this resource.
	 *
	 * @param hubConfig A hub config object containing parameters required for this auth flow
	 * @return An authorization flow
	 * @throws Exception In case of any problems starting the server
	 */
	public static AuthFlow init(HubConfig hubConfig, RedirectContext redirectContext) throws Exception {
		var receiver = AuthFlowReceiver.start(hubConfig, redirectContext);
		return new AuthFlow(receiver, hubConfig);
	}

	/**
	 * Runs this Authorization Code Flow. This will take a long time and should be done in a background thread.
	 *
	 * @param browser A callback that will open the auth URI in a browser
	 * @return The access token
	 * @throws IOException In case of any errors, including failed authentication.
	 * @throws InterruptedException If this method is interrupted while waiting for responses from the authorization server
	 */
	public String run(Consumer<URI> browser) throws IOException, InterruptedException {
		var pkce = new PKCE();
		var authCode = auth(pkce, browser);
		return token(pkce, authCode);
	}

	private String auth(PKCE pkce, Consumer<URI> browser) throws IOException, InterruptedException {
		var state = BASE64URL.encode(randomBytes(16));
		receiver.prepareReceive(state);
		var params = Map.of("response_type", "code", //
				"client_id", clientId, //
				"redirect_uri", receiver.getRedirectUri(), //
				"state", state, //
				"code_challenge", pkce.challenge, //
				"code_challenge_method", PKCE.METHOD //
		);
		var uri = appendQueryParams(this.authEndpoint, params);

		// open browser and wait for response
		LOG.debug("waiting for user to log into {}", uri);
		browser.accept(uri);
		var callback = receiver.receive();

		if (!state.equals(callback.state())) {
			throw new IOException("Invalid CSRF Token");
		} else if (callback.error() != null) {
			throw new IOException("Authentication failed " + callback.error());
		} else if (callback.code() == null) {
			throw new IOException("Received neither authentication code nor error");
		}
		return callback.code();
	}

	private String token(PKCE pkce, String authCode) throws IOException, InterruptedException {
		var params = Map.of("grant_type", "authorization_code", //
				"client_id", "cryptomator-hub", // TODO
				"redirect_uri", receiver.getRedirectUri(), //
				"code", authCode, //
				"code_verifier", pkce.verifier //
		);
		var paramStr = paramString(params).collect(Collectors.joining("&"));
		var request = HttpRequest.newBuilder(this.tokenEndpoint) //
				.header("Content-Type", "application/x-www-form-urlencoded") //
				.POST(HttpRequest.BodyPublishers.ofString(paramStr)) //
				.build();
		HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() == 200) {
			var json = HttpHelper.parseBody(response);
			return json.getAsJsonObject().get("access_token").getAsString();
		} else {
			LOG.error("Unexpected HTTP response {}: {}", response.statusCode(), HttpHelper.readBody(response));
			throw new IOException("Unexpected HTTP response code " + response.statusCode());
		}
	}

	private URI appendQueryParams(URI uri, Map<String, String> params) {
		var oldParams = Splitter.on("&").omitEmptyStrings().splitToStream(Strings.nullToEmpty(uri.getQuery()));
		var newParams = paramString(params);
		var query = Streams.concat(oldParams, newParams).collect(Collectors.joining("&"));
		try {
			return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to create URI from given", e);
		}
	}

	private Stream<String> paramString(Map<String, String> params) {
		return params.entrySet().stream().map(param -> {
			var key = QUERY_STRING_ESCAPER.escape(param.getKey());
			var value = QUERY_STRING_ESCAPER.escape(param.getValue());
			return key + "=" + value;
		});
	}

	@Override
	public void close() throws Exception {
		receiver.close();
	}

	/**
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7636">RFC 7636</a>
	 */
	private static record PKCE(String challenge, String verifier) {

		public static final String METHOD = "S256";

		public PKCE(String verifier) {
			this(BASE64URL.encode(sha256(verifier.getBytes(StandardCharsets.US_ASCII))), verifier);
		}

		public PKCE() {
			this(BASE64URL.encode(randomBytes(32)));
		}

	}

	private static byte[] randomBytes(int len) {
		byte[] bytes = new byte[len];
		CSPRNG.nextBytes(bytes);
		return bytes;
	}

	private static byte[] sha256(byte[] input) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support SHA-256.");
		}
	}

}
