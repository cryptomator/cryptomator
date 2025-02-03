package org.cryptomator.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public record Hyperlinks(String docsVolumeType, String docsGettingStarted, String homepageHub) {

	private static final ObjectMapper JSON_DESERIALIZER = new ObjectMapper();
	/*
	String docsAccessingVaults;
	String docsExpertSettings;
	String docsManualMigration;
	String homepageDownload;
	String homepageHub;
	String homepageDonate;
	String homepageSponsors;
	String storeDesktop;
	 */


	public static Hyperlinks load() {
		try {
			return JSON_DESERIALIZER.readValue(Hyperlinks.class.getResource("/hyperlinks.json"), Hyperlinks.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
