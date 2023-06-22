package org.cryptomator.ui.keyloading.hub;

import java.time.Instant;

class CreateDeviceDto {

	public String id;
	public String name;
	public final String type = "DESKTOP";
	public String publicKey;
	public String userKey;
	public String creationTime;
	public String lastSeenTime;

}
