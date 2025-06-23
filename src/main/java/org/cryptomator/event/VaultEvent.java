package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.time.Instant;

public record VaultEvent(Vault v, FilesystemEvent actualEvent, int count) implements Comparable<VaultEvent> {

	public VaultEvent(Vault v, FilesystemEvent actualEvent) {
		this(v, actualEvent, 1);
	}

	@Override
	public int compareTo(VaultEvent other) {
		var timeResult = actualEvent.getTimestamp().compareTo(other.actualEvent().getTimestamp());
		if(timeResult != 0) {
			return timeResult;
		} else {
			return this.equals(other) ? 0 : this.actualEvent.getClass().getName().compareTo(other.actualEvent.getClass().getName());
		}
	}

	public VaultEvent incrementCount(FilesystemEvent update) {
		return new VaultEvent(v, update, count+1);
	}
}
