package org.cryptomator.ui.fxapp;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

abstract class DelegatingHttpClient extends HttpClient {

	private final HttpClient delegate;

	public DelegatingHttpClient(HttpClient delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
	}

	@Override
	public Optional<CookieHandler> cookieHandler() {
		return delegate.cookieHandler();
	}

	@Override
	public Optional<Duration> connectTimeout() {
		return delegate.connectTimeout();
	}

	@Override
	public Redirect followRedirects() {
		return delegate.followRedirects();
	}

	@Override
	public Optional<ProxySelector> proxy() {
		return delegate.proxy();
	}

	@Override
	public SSLContext sslContext() {
		return delegate.sslContext();
	}

	@Override
	public SSLParameters sslParameters() {
		return delegate.sslParameters();
	}

	@Override
	public Optional<Authenticator> authenticator() {
		return delegate.authenticator();
	}

	@Override
	public Version version() {
		return delegate.version();
	}

	@Override
	public Optional<Executor> executor() {
		return delegate.executor();
	}

	@Override
	public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
		return delegate.send(request, responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
		return delegate.sendAsync(request, responseBodyHandler);
	}

	@Override
	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
		return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
	}

}
