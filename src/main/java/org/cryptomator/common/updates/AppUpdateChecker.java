package org.cryptomator.common.updates;

import org.cryptomator.integrations.common.DistributionChannel;
import org.cryptomator.integrations.update.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class AppUpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(AppUpdateChecker.class);
	private final List<UpdateService> updateServices;

	@Inject
	public AppUpdateChecker(List<UpdateService> updateServices) {
		this.updateServices = updateServices;
	}

	public boolean isUpdateServiceAvailable(Optional<String> buildNumber) {
		if (buildNumber.isEmpty()) {
			return false;
		}
		switch (buildNumber.get()) {
			case "flatpak-1" -> {
				return !updateServices.isEmpty() && doServicesContainChannel(updateServices, DistributionChannel.Value.LINUX_FLATPAK);
			}

			default -> {
				LOG.error("Unexpected value 'buildNumber': {}", buildNumber.get());
				return false;
			}
		}
	}

	public String checkForUpdates(DistributionChannel.Value channel) {
		if (updateServices.isEmpty()) {
			LOG.error("No UpdateService found");
			return null;
		}
		switch (channel) {
			case LINUX_FLATPAK -> {
				var flatpakService = getServiceForChannel(updateServices, DistributionChannel.Value.LINUX_FLATPAK);
				if(null == flatpakService) {
					LOG.error("Required service for channel LINUX_FLATPAK not available");
					return null;
				} else {
					return flatpakService.isUpdateAvailable(DistributionChannel.Value.LINUX_FLATPAK);
				}
			}
			default -> throw new IllegalStateException("Unexpected value 'channel': " + channel);
		}
	}

	private boolean doServicesContainChannel(List<UpdateService> services, DistributionChannel.Value requiredChannel) {
		return services.stream().anyMatch(service -> {
			DistributionChannel annotation = service.getClass().getAnnotation(DistributionChannel.class);
			return annotation != null && annotation.value() == requiredChannel;
		});
	}

	private UpdateService getServiceForChannel(List<UpdateService> services, DistributionChannel.Value requiredChannel) {
		return services.stream().filter(service -> {
			DistributionChannel annotation = service.getClass().getAnnotation(DistributionChannel.class);
			return annotation != null && annotation.value() == requiredChannel;
		}).findFirst().orElse(null);
	}

}
