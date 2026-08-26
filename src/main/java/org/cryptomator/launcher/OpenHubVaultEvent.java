package org.cryptomator.launcher;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.ui.keyloading.hub.HubConfig;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Requests opening a Hub vault from an {@code org.cryptomator://vault/open#vaultConfig=…} deeplink.
 * <p>
 * The single parameter is the vault's {@code vault.cryptomator}, a compact JWS embedded verbatim - a compact JWS
 * consists only of characters that are unreserved in a URI fragment, so it needs no further encoding. It is carried in
 * the fragment part so that it stays out of the logs of any web page that mints such a link.
 * <p>
 * The config is read <em>unverified</em>: its signature is keyed on the masterkey, which is only obtainable from Hub
 * later on. Everything consumed before that point is therefore validated here rather than trusted.
 *
 * @param vaultConfig the decoded, unverified vault config
 * @param vaultId     the vault's id within its Hub instance, taken from the config's key id
 */
public record OpenHubVaultEvent(VaultConfig.UnverifiedVaultConfig vaultConfig, UUID vaultId) implements AppLaunchEvent {

	private static final Logger LOG = LoggerFactory.getLogger(OpenHubVaultEvent.class);
	private static final String SCHEME = "org.cryptomator";
	private static final String HOST = "vault";
	private static final String PATH = "/open";
	private static final String PARAM_VAULT_CONFIG = "vaultConfig";
	private static final String HUB_HEADER = "hub";
	private static final int MAX_CONFIG_LENGTH = 8192; //real Hub vault config is ~1KB leaving some room for extensions

	/**
	 * Attempts to interpret the given URI as an {@code org.cryptomator://vault/open#vaultConfig=…} deeplink.
	 *
	 * @param uri the deeplink URI
	 * @return the parsed event, or an empty optional if the URI's scheme, host or path do not identify a vault-open
	 * deeplink
	 * @throws IllegalArgumentException if the URI identifies a vault-open deeplink, but the config is missing, too
	 *                                  large, not decodable, or does not describe a Hub vault
	 */
	public static Optional<OpenHubVaultEvent> tryParse(URI uri) {
		if (!SCHEME.equalsIgnoreCase(uri.getScheme()) || !HOST.equalsIgnoreCase(uri.getHost()) || !PATH.equals(uri.getPath())) {
			return Optional.empty();
		}
		var params = parseParams(uri.getRawFragment());

		var token = params.get(PARAM_VAULT_CONFIG);
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("Missing required fragment parameter '" + PARAM_VAULT_CONFIG + "'.");
		}
		var vaultConfig = decode(token);
		requireHubVault(vaultConfig);
		var vaultId = extractVaultId(vaultConfig.getKeyId());

		var leftoverParams = params.keySet().stream().filter(k -> !k.equals(PARAM_VAULT_CONFIG)).toList();
		if (!leftoverParams.isEmpty()) {
			LOG.debug("Ignoring unknown parameters {}", leftoverParams);
		}

		return Optional.of(new OpenHubVaultEvent(vaultConfig, vaultId));
	}

	private static VaultConfig.UnverifiedVaultConfig decode(String token) {
		// a compact JWS is ASCII, so its character count is its byte count
		if (token.length() > MAX_CONFIG_LENGTH) {
			throw new IllegalArgumentException("Fragment parameter '%s' must not exceed %d bytes.".formatted(PARAM_VAULT_CONFIG, MAX_CONFIG_LENGTH));
		}
		try {
			return VaultConfig.decode(token);
		} catch (VaultConfigLoadException e) {
			throw new IllegalArgumentException("Fragment parameter '" + PARAM_VAULT_CONFIG + "' is not a decodable vault config.", e);
		}
	}

	/**
	 * Ensures the config describes a Hub vault and that the endpoints the app will talk to are usable.
	 */
	private static void requireHubVault(VaultConfig.UnverifiedVaultConfig vaultConfig) {
		var keyIdScheme = vaultConfig.getKeyId().getScheme();
		if (keyIdScheme == null || !keyIdScheme.startsWith(HubKeyLoadingStrategy.SCHEME_PREFIX)) {
			throw new IllegalArgumentException("Vault config does not describe a Hub vault, but had key id scheme '" + keyIdScheme + "'.");
		}
		HubConfig hubConfig;
		try {
			hubConfig = vaultConfig.getHeader(HUB_HEADER, HubConfig.class);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Vault config contains an unreadable '" + HUB_HEADER + "' header.", e);
		}
		if (hubConfig == null) {
			throw new IllegalArgumentException("Vault config contains no '" + HUB_HEADER + "' header.");
		}
		URI apiBaseUrl;
		try {
			apiBaseUrl = hubConfig.getApiBaseUrl();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Vault config declares no usable hub api base url.", e);
		}
		requireUsableEndpoint("apiBaseUrl", apiBaseUrl);
		requireUsableEndpoint("authEndpoint", toUri("authEndpoint", hubConfig.authEndpoint));
	}

	private static URI toUri(String field, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Vault config declares no hub " + field + ".");
		}
		try {
			return URI.create(value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Vault config's hub " + field + " is not a valid url, but was '" + value + "'.", e);
		}
	}

	private static void requireUsableEndpoint(String field, URI uri) {
		if (!uri.isAbsolute() || uri.getHost() == null) {
			throw new IllegalArgumentException("Vault config's hub " + field + " is not an absolute url with a host, but was '" + uri + "'.");
		}
		// Whether an http host is acceptable (it is, for local development) is decided by CheckHostTrustController, the
		// authority on host trust. Here we only ensure the endpoint is shaped like something that decision can be made on.
		var scheme = uri.getScheme();
		if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
			throw new IllegalArgumentException("Vault config's hub " + field + " is neither http nor https, but was '" + uri + "'.");
		}
	}

	/**
	 * Reads the vault id from the trailing path segment of a key id such as
	 * {@code hub+https://hub.example.com/api/vaults/<vaultId>}.
	 * <p>
	 * Requiring a UUID matters beyond well-formedness: the id is interpolated into the {@code api/vaults/{vaultId}/…}
	 * request path, so it must not be able to introduce a path segment. Re-serializing the parsed {@link UUID} rather
	 * than passing the raw segment on keeps that guarantee.
	 */
	private static UUID extractVaultId(URI keyId) {
		var path = keyId.getPath();
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("Vault config's key id contains no vault id, but was '" + keyId + "'.");
		}
		var lastSegment = path.substring(path.lastIndexOf('/') + 1);
		try {
			return UUID.fromString(lastSegment);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Vault config's key id does not end in a vault id, but was '" + keyId + "'.", e);
		}
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
