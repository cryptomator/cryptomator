package org.cryptomator.common.networking;

import org.cryptomator.integrations.common.IntegrationsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public interface SSLContextProvider {

	Logger LOG = LoggerFactory.getLogger(SSLContextProvider.class);

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
