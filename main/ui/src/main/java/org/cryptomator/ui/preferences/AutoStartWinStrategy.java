package org.cryptomator.ui.preferences;

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

	public AutoStartWinStrategy(String exePath) {
		this.exePath = exePath;
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
				throw new TogglingAutoStartFailedException("Adding registry value failed.");
			}
		} catch (IOException e) {
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
				throw new TogglingAutoStartFailedException("Removing registry value failed.");
			}
		} catch (IOException e) {
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

}
