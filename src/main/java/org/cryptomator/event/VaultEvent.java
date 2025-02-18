package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.time.Instant;

public record VaultEvent(long timestamp, Vault v, FilesystemEvent actualEvent) implements Event {

	public VaultEvent(Vault v, FilesystemEvent actualEvent) {
		this(Instant.now().toEpochMilli(), v, actualEvent);
	}

	@Override
	public long getTimestampMilli() {
		return timestamp;
	}
}
