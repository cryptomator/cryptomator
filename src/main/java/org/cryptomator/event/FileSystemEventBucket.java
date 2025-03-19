package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

public record FileSystemEventBucket(Vault v, FilesystemEvent mostRecent, int count) implements Comparable<FileSystemEventBucket> {

	@Override
	public int compareTo(FileSystemEventBucket other) {
		var timeResult = mostRecent.getTimestamp().compareTo(other.mostRecent().getTimestamp());
		if (timeResult != 0) {
			return timeResult;
		}
		var vaultIdResult = v.getId().compareTo(other.v.getId());
		if (vaultIdResult != 0) {
			return vaultIdResult;
		}
		return this.mostRecent.getClass().getName().compareTo(other.mostRecent.getClass().getName());
	}

}
