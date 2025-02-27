package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.time.Instant;

public record VaultEvent(Instant timestamp, Vault v, FilesystemEvent actualEvent) implements Comparable<VaultEvent> {

	public VaultEvent(Vault v, FilesystemEvent actualEvent) {
		this(Instant.now(), v, actualEvent);
	}

	@Override
	public int compareTo(VaultEvent other) {
		var timeResult = this.timestamp.compareTo(other.timestamp);
		if(timeResult != 0) {
			return timeResult;
		} else {
			return this.equals(other) ? 0 : this.actualEvent.getClass().getName().compareTo(other.actualEvent.getClass().getName());
		}
	}
}
