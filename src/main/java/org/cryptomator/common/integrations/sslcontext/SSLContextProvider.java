package org.cryptomator.common.integrations.sslcontext;

import org.cryptomator.common.integrations.IntegrationsLoaderCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
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
		return IntegrationsLoaderCopy.loadAll(SSLContextProvider.class);
	}
}
