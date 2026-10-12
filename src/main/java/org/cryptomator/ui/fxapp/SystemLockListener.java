package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.AutoLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.Desktop;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;

/**
 * Listens for the OS locking the user session (e.g. screen lock, user switch) or going to sleep
 * and asks the {@link AutoLocker} to lock all vaults that opted in via
 * {@link org.cryptomator.common.settings.VaultSettings#autoLockOnSystemLock}.
 * <p>
 * Relies on {@link java.awt.desktop} app events, which are supported by the JDK on Windows and macOS.
 */
@FxApplicationScoped
public class SystemLockListener {

	private static final Logger LOG = LoggerFactory.getLogger(SystemLockListener.class);

	private final AutoLocker autoLocker;

	@Inject
	SystemLockListener(AutoLocker autoLocker) {
		this.autoLocker = autoLocker;
	}

	/**
	 * @return <code>true</code> if the current platform reports session lock or system sleep events
	 */
	public static boolean isSupported() {
		if (!Desktop.isDesktopSupported()) {
			return false;
		}
		var desktop = Desktop.getDesktop();
		return desktop.isSupported(Desktop.Action.APP_EVENT_USER_SESSION) || desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP);
	}

	public void initialize() {
		if (!Desktop.isDesktopSupported()) {
			LOG.debug("java.awt.Desktop not supported. Vaults will not be locked on system lock.");
			return;
		}
		var desktop = Desktop.getDesktop();

		if (desktop.isSupported(Desktop.Action.APP_EVENT_USER_SESSION)) {
			desktop.addAppEventListener(new UserSessionListener() {
				@Override
				public void userSessionDeactivated(UserSessionEvent e) {
					LOG.debug("User session deactivated (reason: {})", e.getReason());
					autoLocker.lockOnSystemLock("session lock (" + e.getReason() + ")");
				}

				@Override
				public void userSessionActivated(UserSessionEvent e) {
					// no-op
				}
			});
		}

		if (desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) {
			desktop.addAppEventListener(new SystemSleepListener() {
				@Override
				public void systemAboutToSleep(SystemSleepEvent e) {
					LOG.debug("System about to sleep");
					autoLocker.lockOnSystemLock("system sleep");
				}

				@Override
				public void systemAwoke(SystemSleepEvent e) {
					// no-op
				}
			});
		}
	}

}
