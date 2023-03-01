package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountService;

public record ActualMountService(MountService service, boolean isDesired) {
}
