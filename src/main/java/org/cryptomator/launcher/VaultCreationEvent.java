package org.cryptomator.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Requests creation of a new vault from a {@code cryptomator://vault/create?name=…&template=…} deeplink.
 * <p>
 * The {@code template} is a Base64URL-encoded ZIP archive holding a ready-made (signed) vault. The {@code name} fixes
 * the vault's directory name and is restricted to a single, safe path segment so that resolving it against a
 * user-chosen location cannot escape that location.
 *
 * @param name     the fixed vault name (a single, safe path segment)
 * @param template the Base64URL-decoded vault template
 */
public record VaultCreationEvent(String name, byte[] template) implements AppLaunchEvent {

	private static final Logger LOG = LoggerFactory.getLogger(VaultCreationEvent.class);
	private static final String SCHEME = "cryptomator";
	private static final String HOST = "vault";
	private static final String PATH = "/create";

	/**
	 * Attempts to interpret the given URI as a {@code cryptomator://vault/create?name=…&template=…} deeplink.
	 *
	 * @param uri the deeplink URI
	 * @return the parsed event, or an empty optional if the URI's scheme, host or path do not identify a
	 * vault-creation deeplink
	 * @throws IllegalArgumentException if the URI identifies a vault-creation deeplink, but a required parameter is
	 *                                  missing, the template is not valid Base64URL, or the name is not a single safe
	 *                                  path segment
	 */
	public static Optional<VaultCreationEvent> tryParse(URI uri) {
		if (!SCHEME.equalsIgnoreCase(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost()) || !PATH.equals(uri.getPath())) {
			return Optional.empty();
		}
		var params = parseQuery(uri.getRawQuery());
		var name = params.get("name");
		var templateParam = params.get("template");
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Missing required query parameter 'name'.");
		}
		if (templateParam == null || templateParam.isEmpty()) {
			throw new IllegalArgumentException("Missing required query parameter 'template'.");
		}
		validateName(name);
		byte[] template;
		try {
			template = Base64.getUrlDecoder().decode(templateParam);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Query parameter 'template' is not valid Base64URL.", e);
		}
		var leftoverParams = params.keySet().stream().filter(k -> !(k.equals("template") || k.equals("name"))).toList();
		if (!leftoverParams.isEmpty()) {
			LOG.debug("Ignoring unknown parameters {}", leftoverParams);
		}
		return Optional.of(new VaultCreationEvent(name, template));
	}

	private static void validateName(String name) {
		if (name.contains("/") || name.contains("\\") || name.contains("..") || name.equals(".")) {
			throw new IllegalArgumentException("Query parameter 'name' must be a single path segment, but was '" + name + "'.");
		}
	}

	private static Map<String, String> parseQuery(String rawQuery) {
		var params = new HashMap<String, String>();
		if (rawQuery == null || rawQuery.isEmpty()) {
			return params;
		}
		for (var pair : rawQuery.split("&")) {
			var idx = pair.indexOf('=');
			if (idx < 0) {
				continue;
			}
			var key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
			var value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
			params.put(key, value);
		}
		return params;
	}

}
