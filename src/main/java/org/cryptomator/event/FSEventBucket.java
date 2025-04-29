package org.cryptomator.event;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import java.nio.file.Path;

public record FSEventBucket(Vault vault, Path idPath, Class<? extends FilesystemEvent> c) {}
