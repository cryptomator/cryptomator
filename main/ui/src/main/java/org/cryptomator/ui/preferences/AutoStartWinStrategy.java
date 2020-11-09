package org.cryptomator.ui.preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * OS specific class to check, en- and disable the auto start on Windows.
 * <p>
 * Two strategies are implemented for this feature, the first uses the registry and the second one the autostart folder.
 * <p>
 * The registry strategy checks/add/removes at the registry key {@value HKCU_AUTOSTART_KEY} an entry for Cryptomator.
 * The folder strategy checks/add/removes at the location {@value WINDOWS_START_MENU_ENTRY}.
 * <p>
 * To check if the feature is active, both strategies are applied.
 * To enable the feature, first the registry is tried and only on failure the autostart folder is used.
 * To disable it, first it is determined by an internal state, which strategies must be used and in the second step those are executed.
 *
 * @apiNote This class is not thread safe, hence it should be avoided to call its methods simultaniously by different threads.
 * @deprecated To be moved to integration-win project
 */
@Deprecated
class AutoStartWinStrategy implements AutoStartStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AutoStartWinStrategy.class);
	private static final String HKCU_AUTOSTART_KEY = "\"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\"";
	private static final String AUTOSTART_VALUE = "Cryptomator";
	private static final String WINDOWS_START_MENU_ENTRY = "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Cryptomator.lnk";

	private final String exePath;

	private boolean activatedUsingFolder;
	private boolean activatedUsingRegistry;

	public AutoStartWinStrategy(String exePath) {
		this.exePath = exePath;
		this.activatedUsingFolder = false;
		this.activatedUsingRegistry = false;
	}

	@Override
	public CompletionStage<Boolean> isAutoStartEnabled() {
		return isAutoStartEnabledUsingRegistry().thenCombine(isAutoStartEnabledUsingFolder(), (bReg, bFolder) -> bReg || bFolder);
	}

	private CompletableFuture<Boolean> isAutoStartEnabledUsingFolder() {
		Path autoStartEntry = Path.of(System.getProperty("user.home") + WINDOWS_START_MENU_ENTRY);
		this.activatedUsingFolder = Files.exists(autoStartEntry);
		return CompletableFuture.completedFuture(activatedUsingFolder);
	}

	private CompletableFuture<Boolean> isAutoStartEnabledUsingRegistry() {
		ProcessBuilder regQuery = new ProcessBuilder("reg", "query", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE);
		try {
			Process proc = regQuery.start();
			return proc.onExit().thenApply(p -> {
				this.activatedUsingRegistry = p.exitValue() == 0;
				return activatedUsingRegistry;
			});
		} catch (IOException e) {
			LOG.debug("Failed to query {} from registry key {}", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
			return CompletableFuture.completedFuture(false);
		}
	}

	@Override
	public void enableAutoStart() throws TogglingAutoStartFailedException {
		try {
			enableAutoStartUsingRegistry().thenAccept((Void v) -> this.activatedUsingRegistry = true).exceptionallyCompose(e -> {
				LOG.debug("Falling back to using autostart folder.");
				return this.enableAutoStartUsingFolder();
			}).thenAccept((Void v) -> this.activatedUsingFolder = true).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TogglingAutoStartFailedException("Execution of enabling auto start setting was interrupted.");
		} catch (ExecutionException e) {
			throw new TogglingAutoStartFailedException("Enabling auto start failed both using registry and auto start folder.");
		}
	}

	private CompletableFuture<Void> enableAutoStartUsingRegistry() {
		ProcessBuilder regAdd = new ProcessBuilder("reg", "add", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE, //
				"/t", "REG_SZ", //
				"/d", "\"" + exePath + "\"", //
				"/f");
		try {
			Process proc = regAdd.start();
			boolean finishedInTime = waitForProcessOrCancel(proc, 5, TimeUnit.SECONDS);
			if (finishedInTime && proc.exitValue() == 0) {
				LOG.debug("Added {} to registry key {}.", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
				return CompletableFuture.completedFuture(null);
			} else {
				throw new IOException("Process exited with error code " + proc.exitValue());
			}
		} catch (IOException e) {
			LOG.debug("Registry could not be edited to set auto start.", e);
			return CompletableFuture.failedFuture(new SystemCommandException("Adding registry value failed."));
		}
	}

	private CompletableFuture<Void> enableAutoStartUsingFolder() {
		String autoStartFolderEntry = System.getProperty("user.home") + WINDOWS_START_MENU_ENTRY;
		String createShortcutCommand = "$s=(New-Object -COM WScript.Shell).CreateShortcut('" + autoStartFolderEntry + "');$s.TargetPath='" + exePath + "';$s.Save();";
		ProcessBuilder shortcutAdd = new ProcessBuilder("cmd", "/c", "Start powershell " + createShortcutCommand);
		try {
			Process proc = shortcutAdd.start();
			boolean finishedInTime = waitForProcessOrCancel(proc, 5, TimeUnit.SECONDS);
			if (finishedInTime && proc.exitValue() == 0) {
				LOG.debug("Created file {} for auto start.", autoStartFolderEntry);
				return CompletableFuture.completedFuture(null);
			} else {
				throw new IOException("Process exited with error code " + proc.exitValue());
			}
		} catch (IOException e) {
			LOG.debug("Adding entry to auto start folder failed.", e);
			return CompletableFuture.failedFuture(new SystemCommandException("Adding entry to auto start folder failed."));
		}
	}


	@Override
	public void disableAutoStart() throws TogglingAutoStartFailedException {
		if (activatedUsingRegistry) {
			disableAutoStartUsingRegistry().whenComplete((voit, ex) -> {
				if (ex == null) {
					this.activatedUsingRegistry = false;
				}
			});
		}

		if (activatedUsingFolder) {
			disableAutoStartUsingFolder().whenComplete((voit, ex) -> {
				if (ex == null) {
					this.activatedUsingFolder = false;
				}
			});
		}

		if (activatedUsingRegistry || activatedUsingFolder) {
			throw new TogglingAutoStartFailedException("Disabling auto start failed using registry and/or auto start folder.");
		}
	}

	public CompletableFuture<Void> disableAutoStartUsingRegistry() {
		ProcessBuilder regRemove = new ProcessBuilder("reg", "delete", HKCU_AUTOSTART_KEY, //
				"/v", AUTOSTART_VALUE, //
				"/f");
		try {
			Process proc = regRemove.start();
			boolean finishedInTime = waitForProcessOrCancel(proc, 5, TimeUnit.SECONDS);
			if (finishedInTime && proc.exitValue() == 0) {
				LOG.debug("Removed {} from registry key {}.", AUTOSTART_VALUE, HKCU_AUTOSTART_KEY);
				return CompletableFuture.completedFuture(null);
			} else {
				throw new IOException("Process exited with error code " + proc.exitValue());
			}
		} catch (IOException e) {
			LOG.debug("Registry could not be edited to remove auto start.", e);
			return CompletableFuture.failedFuture(new SystemCommandException("Removing registry value failed."));
		}
	}

	private CompletableFuture<Void> disableAutoStartUsingFolder() {
		try {
			Files.delete(Path.of(WINDOWS_START_MENU_ENTRY));
			LOG.debug("Successfully deleted {}.", WINDOWS_START_MENU_ENTRY);
			return CompletableFuture.completedFuture(null);
		} catch (NoSuchFileException e) {
			//that is also okay
			return CompletableFuture.completedFuture(null);
		} catch (IOException e) {
			LOG.debug("Failed to delete entry from auto start folder.", e);
			return CompletableFuture.failedFuture(e);
		}
	}

	private static boolean waitForProcessOrCancel(Process proc, int timeout, TimeUnit timeUnit) {
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

	private class SystemCommandException extends RuntimeException {

		public SystemCommandException(String msg) {
			super(msg);
		}
	}

}
