package org.cryptomator.updater;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckerHttpClient extends DelegatingHttpClient {

	private final String userAgent;

	public UpdateCheckerHttpClient(Environment env) {
		var delegate = HttpClient.newBuilder() //
				.followRedirects(HttpClient.Redirect.NORMAL) // from version 1.6.11 onwards, Cryptomator can follow redirects, in case this URL ever changes
				.proxy(ProxySelector.getDefault()).build();
		super(delegate);
		this.userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", env.getAppVersion(), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);
	}

	@Override
	public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
		return super.send(decorateRequest(request), responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
		return super.sendAsync(decorateRequest(request), responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
		return super.sendAsync(decorateRequest(request), responseBodyHandler, pushPromiseHandler);
	}

	private HttpRequest decorateRequest(HttpRequest request) {
		return HttpRequest.newBuilder(request, (_, _) -> true) //
				.header("User-Agent", this.userAgent) //
				.timeout(Duration.ofSeconds(10)) //
				.build();

	}


}
