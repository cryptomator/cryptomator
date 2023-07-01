package org.cryptomator.ui.keyloading.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// needs to be accessible by JSON decoder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubConfig {

	public String clientId;
	public String authEndpoint;
	public String tokenEndpoint;
	public String devicesResourceUrl;
	public String authSuccessUrl;
	public String authErrorUrl;

}
