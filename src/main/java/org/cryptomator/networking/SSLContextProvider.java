package org.cryptomator.networking;

import org.cryptomator.integrations.common.IntegrationsLoader;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public interface SSLContextProvider {

	SSLContext getContext(SecureRandom csprng) throws SSLContextBuildException;

	class SSLContextBuildException extends Exception {

		SSLContextBuildException(Throwable t) {
			super(t);
		}
	}

	static Stream<SSLContextProvider> loadAll() {
		return IntegrationsLoader.loadAll(ServiceLoader.load(SSLContextProvider.class), SSLContextProvider.class);
	}
}
