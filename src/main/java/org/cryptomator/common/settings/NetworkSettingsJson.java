package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class NetworkSettingsJson {

	@JsonProperty("mode")
	NetworkSettings.ProxyMode mode = NetworkSettings.ProxyMode.NO;

	@JsonProperty("httpProxy")
	String httpProxy;

	@JsonProperty("httpPort")
	int httpPort;

	@JsonProperty("samePortProxyForHttpHttps")
	boolean samePortProxyForHttpHttps;

	@JsonProperty(value = "httpsProxy")
	String httpsProxy;

	@JsonProperty("httpsPort")
	int httpsPort;

}
