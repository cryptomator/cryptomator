package org.cryptomator.common.updates;

import org.cryptomator.integrations.update.UpdateFailedException;
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

	public void checkForUpdates() {
		updateService.ifPresent(service -> {
			try {
				service.triggerUpdate();
			} catch (UpdateFailedException e) {
				LOG.error(e.toString(), e.getCause());
			}
		});
	}
}
