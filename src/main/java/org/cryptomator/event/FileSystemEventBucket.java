package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

public record FileSystemEventBucket(Vault vault, FilesystemEvent mostRecent, int count) implements Comparable<FileSystemEventBucket> {

	@Override
	public boolean equals(Object other) {
		if (other instanceof FileSystemEventBucket(Vault v2, FilesystemEvent e2, _)) {
			return vault.equals(v2) && mostRecent.getClass().equals(e2.getClass());
		}
		return false;
	}

	@Override
	public int compareTo(FileSystemEventBucket other) {
		var timeResult = mostRecent.getTimestamp().compareTo(other.mostRecent().getTimestamp());
		if (timeResult != 0) {
			return timeResult;
		}
		var vaultIdResult = vault.getId().compareTo(other.vault.getId());
		if (vaultIdResult != 0) {
			return vaultIdResult;
		}
		return this.mostRecent.getClass().getName().compareTo(other.mostRecent.getClass().getName());
	}

}
