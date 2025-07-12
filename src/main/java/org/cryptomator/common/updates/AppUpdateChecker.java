package org.cryptomator.common.updates;

import org.cryptomator.integrations.update.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class AppUpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(AppUpdateChecker.class);
	private final Optional<UpdateService> updateService;

	@Inject
	public AppUpdateChecker(Optional<UpdateService> updateService) {
		this.updateService = updateService;
	}

	public boolean isUpdateServiceAvailable() {
		return updateService.isPresent();
	}

	public String checkForUpdates(UpdateService.DistributionChannel channel) {
		if (!updateService.isPresent()) {
			LOG.error("No UpdateService found");
			return null;
		}
		switch (channel) {
			case LINUX_FLATPAK -> {
				return updateService.map(service -> service.isUpdateAvailable(UpdateService.DistributionChannel.LINUX_FLATPAK)).orElse(null);
			}
			default -> throw new IllegalStateException("Unexpected value: " + channel);
		}
	}
}
