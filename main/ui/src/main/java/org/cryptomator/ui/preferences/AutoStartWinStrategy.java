package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class AutoStartWinStrategy implements AutoStartStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AutoStartWinStrategy.class);
	private static final String HKCU_AUTOSTART_KEY = "\"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\"";
	private static final String AUTOSTART_VALUE = "Cryptomator";
	private final String exePath;
	private static final String WINDOWS_START_MENU_FOLDER = "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs";
	private Environment env;

	public AutoStartWinStrategy(String exePath, Environment env) {
		this.exePath = exePath;
		this.env = env;
	}

	@Override
	public CompletionStage<Boolean> isAutoStartEnabled() {
		ProcessBuilder regQuery = new ProcessBuilder("reg", "query", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE);
		try {
			Process proc = regQuery.start();
			return proc.onExit().thenApply(p -> p.exitValue() == 0);
		} catch (IOException e) {
			LOG.warn("Failed to query {} from registry key {}", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
			return CompletableFuture.completedFuture(false);
		}
	}

	@Override
	public void enableAutoStart() throws TogglingAutoStartFailedException {
		ProcessBuilder regAdd = new ProcessBuilder("reg", "add", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE, //
				"/t", "REG_SZ", //
				"/d", "\"" + exePath + "\"", //
				"/f");
		String command = regAdd.command().stream().collect(Collectors.joining(" "));
		try {
			Process proc = regAdd.start();
			boolean finishedInTime = waitForProcess(proc, 5, TimeUnit.SECONDS);
			if (finishedInTime) {
				LOG.debug("Added {} to registry key {}.", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
			} else {
				addShortcutOfAppToAutostartFolder();
				throw new TogglingAutoStartFailedException("Adding registry value failed.");
			}
		} catch (IOException e) {
			addShortcutOfAppToAutostartFolder();
			throw new TogglingAutoStartFailedException("Adding registry value failed. " + command, e);
		}
	}

	@Override
	public void disableAutoStart() throws TogglingAutoStartFailedException {
		ProcessBuilder regRemove = new ProcessBuilder("reg", "delete", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE, //
				"/f");
		String command = regRemove.command().stream().collect(Collectors.joining(" "));
		try {
			Process proc = regRemove.start();
			boolean finishedInTime = waitForProcess(proc, 5, TimeUnit.SECONDS);
			if (finishedInTime) {
				LOG.debug("Removed {} from registry key {}.", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
			} else {
				removeShortcutOfAppFromAutostartFolder();
				throw new TogglingAutoStartFailedException("Removing registry value failed.");
			}
		} catch (IOException e) {
			removeShortcutOfAppFromAutostartFolder();
			throw new TogglingAutoStartFailedException("Removing registry value failed. " + command, e);
		}
	}

	private static boolean waitForProcess(Process proc, int timeout, TimeUnit timeUnit) {
		boolean finishedInTime = false;
		try {
			finishedInTime = proc.waitFor(timeout, timeUnit);
		} catch (InterruptedException e) {
			LOG.error("Timeout while reading registry", e);
			Thread.currentThread().interrupt();
		} finally {
			if (!finishedInTime) {
				proc.destroyForcibly();
			}
		}
		return finishedInTime;
	}

	private void addShortcutOfAppToAutostartFolder() throws TogglingAutoStartWithPowershellFailedException{
		String startmenueDirectory = System.getProperty("user.home") + WINDOWS_START_MENU_FOLDER + "\\Cryptomator.lnk";
		String cryptomator = env.getBinaryPath().get().toString();
		String createShortcutCommand = "$s=(New-Object -COM WScript.Shell).CreateShortcut('" + startmenueDirectory + "');$s.TargetPath='" + cryptomator + "';$s.Save();";
		ProcessBuilder shortcutAdd = new ProcessBuilder("cmd", "/c", "Start powershell " + createShortcutCommand);
		try {
			shortcutAdd.start();
		} catch (IOException e) {
			throw new TogglingAutoStartWithPowershellFailedException("Adding shortcut to autostart folder failed.", e);
		}
	}

	private void removeShortcutOfAppFromAutostartFolder() throws TogglingAutoStartWithPowershellFailedException{
		String startmenueDirectory = System.getProperty("user.home") + WINDOWS_START_MENU_FOLDER + "\\Cryptomator.lnk";
		ProcessBuilder shortcutRemove = new ProcessBuilder("cmd", "/c del \"" + startmenueDirectory + "\"");
		try {
			shortcutRemove.start();
		} catch (IOException e) {
			throw new TogglingAutoStartWithPowershellFailedException("Removing shortcut from autostart folder failed.", e);
		}
	}


}
