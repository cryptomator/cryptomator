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
 * Requests creation of a new vault from a {@code cryptomator://vault/create#name=…&template=…} deeplink.
 * <p>
 * The {@code template} is a Base64URL-encoded ZIP archive holding a ready-made (signed) vault. The {@code name} fixes
 * the vault's directory name and is restricted to a single, safe path segment. Both parameters are carried in the URI
 * <em>fragment</em> part.
 *
 * @param name     the fixed vault name (a single, safe path segment)
 * @param template the Base64URL-decoded vault template
 */
public record VaultCreationEvent(String name, byte[] template) implements AppLaunchEvent {

	private static final Logger LOG = LoggerFactory.getLogger(VaultCreationEvent.class);
	private static final int MAX_NAME_LENGTH = 256;
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final String SCHEME = "cryptomator";
	private static final String HOST = "vault";
	private static final String PATH = "/create";

	/**
	 * Attempts to interpret the given URI as a {@code cryptomator://vault/create#name=…&template=…} deeplink.
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
		var params = parseParams(uri.getRawFragment());

		var name = params.get("name");
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Missing required fragment parameter 'name'.");
		}
		validateName(name);

		var templateParam = params.get("template");
		if (templateParam == null || templateParam.isEmpty()) {
			throw new IllegalArgumentException("Missing required fragment parameter 'template'.");
		}
		byte[] template;
		try {
			template = Base64.getUrlDecoder().decode(templateParam);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Fragment parameter 'template' is not valid Base64URL.", e);
		}

		var leftoverParams = params.keySet().stream().filter(k -> !(k.equals("template") || k.equals("name"))).toList();
		if (!leftoverParams.isEmpty()) {
			LOG.debug("Ignoring unknown parameters {}", leftoverParams);
		}

		return Optional.of(new VaultCreationEvent(name, template));
	}

	//TODO: what does Cryptomator Hub allow in vault names?
	private static void validateName(String name) {
		if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("Fragment parameter 'name' must not exceed " + MAX_NAME_LENGTH + " characters.");
		}
		if (name.contains("/") || name.contains("\\") || name.contains(FILE_SEPARATOR) || name.contains("..") || name.equals(".")) {
			throw new IllegalArgumentException("Fragment parameter 'name' must be a single path segment, but was '" + name + "'.");
		}
		if (!name.equals(name.stripTrailing())) {
			// Windows silently strips these, so the directory would not match the requested name
			throw new IllegalArgumentException("Fragment parameter 'name' must not end with whitespace, but was '" + name + "'.");
		}
		// invisible characters (control chars, bidi overrides such as U+202E, zero-width joiners, ...) let a name
		// render deceptively, e.g. "Rechnung<U+202E>gnp.exe" showing up as "Rechnungexe.png"
		var offendingCodePoint = name.codePoints().filter(VaultCreationEvent::isInvisible).findFirst();
		if (offendingCodePoint.isPresent()) {
			throw new IllegalArgumentException("Fragment parameter 'name' must not contain invisible characters, but contained U+%04X.".formatted(offendingCodePoint.getAsInt()));
		}
	}

	private static boolean isInvisible(int codePoint) {
		return switch (Character.getType(codePoint)) {
			case Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.PRIVATE_USE, Character.UNASSIGNED -> true;
			default -> false;
		};
	}

	private static Map<String, String> parseParams(String rawParams) {
		var params = new HashMap<String, String>();
		if (rawParams == null || rawParams.isEmpty()) {
			return params;
		}
		for (var pair : rawParams.split("&")) {
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
