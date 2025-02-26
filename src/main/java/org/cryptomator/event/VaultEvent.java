package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.time.Instant;

public record VaultEvent(long timestamp, Vault v, FilesystemEvent actualEvent) implements Comparable<VaultEvent> {

	public VaultEvent(Vault v, FilesystemEvent actualEvent) {
		this(Instant.now().toEpochMilli(), v, actualEvent);
	}

	@Override
	public int compareTo(VaultEvent other) {
		long timeDiff = this.timestamp - other.timestamp;
		if (timeDiff != 0) {
			return (int) timeDiff;
		}
		return this.equals(other) ? 0 : this.actualEvent.getClass().getName().compareTo(other.actualEvent.getClass().getName());
	}
}
