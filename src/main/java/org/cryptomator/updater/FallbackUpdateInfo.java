package org.cryptomator.updater;

import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateMechanism;

public record FallbackUpdateInfo(String version, UpdateMechanism<FallbackUpdateInfo> updateMechanism) implements UpdateInfo<FallbackUpdateInfo> {}
