package org.cryptomator.common.updates;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.update.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class AppUpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(AppUpdateChecker.class);
	private static final String DISPLAY_NAME_FLATPAK = "Update via Flatpak update";
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
				return !updateServices.isEmpty() && doServicesContainChannel(updateServices, DISPLAY_NAME_FLATPAK);
			}

			default -> {
				LOG.error("Unexpected value 'buildNumber': {}", buildNumber.get());
				return false;
			}
		}
	}

	public Object getUpdater(Optional<String> buildNumber) {
		if (updateServices.isEmpty()) {
			LOG.error("No UpdateService found");
			return null;
		}
		switch (buildNumber.get()) {
			case "flatpak-1" -> {
				var flatpakService = getServiceForChannel(updateServices, DISPLAY_NAME_FLATPAK);
				if(null == flatpakService) {
					LOG.error("Required service for channel LINUX_FLATPAK not available");
					return null;
				} else {
					return flatpakService.getLatestReleaseChecker();
				}
			}
			default -> throw new IllegalStateException("Unexpected value 'buildNumber': " + buildNumber.get());
		}
	}

	private boolean doServicesContainChannel(List<UpdateService> services, String displayName) {
		return services.stream().anyMatch(service -> {
			DisplayName annotation = service.getClass().getAnnotation(DisplayName.class);
			return annotation != null && annotation.value().equals(displayName);
		});
	}

	private UpdateService getServiceForChannel(List<UpdateService> services, String displayName) {
		return services.stream().filter(service -> {
			DisplayName annotation = service.getClass().getAnnotation(DisplayName.class);
			return annotation != null && annotation.value().equals(displayName);
		}).findFirst().orElse(null);
	}

	public UpdateService getServiceForChannel(String displayName) {
		return updateServices.stream().filter(service -> {
			DisplayName annotation = service.getClass().getAnnotation(DisplayName.class);
			return annotation != null && annotation.value().equals(displayName);
		}).findFirst().orElse(null);
	}
}
