package org.cryptomator.launcher;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/**
 * Enumerates the supported deeplink actions and maps an incoming {@link URI} to one of them.
 * <p>
 * A deeplink action is identified by the URI's host and path, e.g. {@code cryptomator://vault/create} maps to
 * {@link #VAULT_CREATE}. Unknown host/path combinations yield an empty {@link Optional} and should be treated as
 * unsupported. New actions are added by extending this enum.
 */
public enum DeeplinkAction {

	VAULT_CREATE("vault", "/create");

	private final String host;
	private final String path;

	DeeplinkAction(String host, String path) {
		this.host = host;
		this.path = path;
	}

	/**
	 * Matches the given URI against the known deeplink actions.
	 *
	 * @param uri the deeplink URI
	 * @return the matching action, or an empty optional if the host/path is not recognized
	 */
	public static Optional<DeeplinkAction> match(URI uri) {
		return Arrays.stream(values()) //
				.filter(action -> action.host.equalsIgnoreCase(uri.getHost()) && action.path.equals(uri.getPath())) //
				.findFirst();
	}

}
