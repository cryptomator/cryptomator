package org.cryptomator.event;

import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.time.Instant;

public record VaultEvent(long timestamp, String vaultId, String displayName, FilesystemEvent actualEvent) implements Event {

	public VaultEvent(String vaultId, String displayName, FilesystemEvent actualEvent) {
		this(Instant.now().toEpochMilli(), vaultId, displayName, actualEvent);
	}

	@Override
	public long getTimestampMilli() {
		return timestamp;
	}
}
