package org.cryptomator.event;

import org.cryptomator.cryptofs.event.FilesystemEvent;

public record FSEventBucketContent(FilesystemEvent mostRecentEvent, int count) {}
