package org.cryptomator.event;

import java.time.Instant;

public record UpdateEvent(long timestamp, String newVersion) implements Event {

	public UpdateEvent(String newVersion) {
		this(Instant.now().toEpochMilli(), newVersion);
	}

	@Override
	public long getTimestampMilli() {
		return timestamp;
	}
}


