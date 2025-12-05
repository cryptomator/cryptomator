package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

public record VaultEvent(Vault v, FilesystemEvent actualEvent) {

}
