package org.cryptomator.notify;

import org.cryptomator.cryptofs.event.FilesystemEvent;

public record VaultEvent(String vaultId, String path, FilesystemEvent actualEvent) implements Event {

}
