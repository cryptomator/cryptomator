package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

public record VaultEvent(Vault v, FilesystemEvent actualEvent, int count) implements Comparable<VaultEvent> {

	@Override
	public int compareTo(VaultEvent other) {
		var timeResult = actualEvent.getTimestamp().compareTo(other.actualEvent().getTimestamp());
		if (timeResult != 0) {
			return timeResult;
		}
		var vaultIdResult = v.getId().compareTo(other.v.getId());
		if (vaultIdResult != 0) {
			return vaultIdResult;
		}
		return this.actualEvent.getClass().getName().compareTo(other.actualEvent.getClass().getName());
	}

}
