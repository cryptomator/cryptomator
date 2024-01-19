package org.cryptomator.ui.keyloading.hub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

// needs to be accessible by JSON decoder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubConfig {

	public String clientId;
	public String authEndpoint;
	public String tokenEndpoint;
	public String authSuccessUrl;
	public String authErrorUrl;
	public @Nullable String apiBaseUrl;
	@Deprecated // use apiBaseUrl + "/devices/"
	public String devicesResourceUrl;

	/**
	 * A collection of String template processors to construct URIs related to this Hub instance.
	 */
	@JsonIgnore
	public final URIProcessors URIs = new URIProcessors();

	/**
	 * Get the URI pointing to the <code>/api/</code> base resource.
	 *
	 * @return <code>/api/</code> URI
	 * @apiNote URI is guaranteed to end on <code>/</code>
	 * @see #URIs
	 */
	public URI getApiBaseUrl() {
		if (apiBaseUrl != null) {
			// make sure to end on "/":
			return URI.create(apiBaseUrl + "/").normalize();
		} else { // legacy approach
			assert devicesResourceUrl != null;
			// make sure to end on "/":
			return URI.create(devicesResourceUrl + "/..").normalize();
		}
	}

	public URI getWebappBaseUrl() {
		return getApiBaseUrl().resolve("../app/");
	}

	public class URIProcessors {

		/**
		 * Resolves paths relative to the <code>/api/</code> endpoint of this Hub instance.
		 */
		public final StringTemplate.Processor<URI, RuntimeException> API = template -> {
			var path = template.interpolate();
			var relPath = path.startsWith("/") ? path.substring(1) : path;
			return getApiBaseUrl().resolve(relPath);
		};

	}
}
