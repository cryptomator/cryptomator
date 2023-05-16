package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.OperatingSystem;
import org.jetbrains.annotations.Blocking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.cryptomator.integrations.common.OperatingSystem.Value.WINDOWS;

@OperatingSystem(WINDOWS)
public final class OneDriveWindowsLocationPresetsProvider implements LocationPresetsProvider {

	private static final String REGSTR_TOKEN = "REG_SZ";
	private static final String REG_ONEDRIVE_ACCOUNTS = "HKEY_CURRENT_USER\\Software\\Microsoft\\OneDrive\\Accounts\\";

	@Override
	public Stream<LocationPreset> getLocations() {
		try {

			var accounts = queryRegistry(REG_ONEDRIVE_ACCOUNTS, List.of(), l -> l.startsWith(REG_ONEDRIVE_ACCOUNTS)).toList();
			var cloudLocations = new ArrayList<LocationPreset>();
			for (var account : accounts) {
				var path = queryRegistry(REG_ONEDRIVE_ACCOUNTS + account, List.of("/v", "UserFolder"), l -> l.contains("UserFolder")).map(result -> result.substring(result.indexOf(REGSTR_TOKEN) + REGSTR_TOKEN.length()).trim()) //
						.map(Path::of) //
						.findFirst().orElseThrow();
				var name = "OneDrive"; //we assume personal oneDrive account by default
				if (!account.equals("Personal")) {
					name = queryRegistry(REG_ONEDRIVE_ACCOUNTS + account, List.of("/v", "DisplayName"), l -> l.contains("DisplayName")).map(result -> result.substring(result.indexOf(REGSTR_TOKEN) + REGSTR_TOKEN.length()).trim()) //
							.map("OneDrive - "::concat).findFirst().orElseThrow();
				}
				cloudLocations.add(new LocationPreset(name, path));
			}
			return cloudLocations.stream();
		} catch (RuntimeException e) {
			return Stream.of();
		}
	}

	private Stream<String> queryRegistry(String keyname, List<String> moreArgs, Predicate<String> outputFilter) {
		var args = new ArrayList<String>();
		args.add("reg");
		args.add("query");
		args.add(keyname);
		args.addAll(moreArgs);
		try {
			ProcessBuilder command = new ProcessBuilder(args);
			Process p = command.start();
			waitForSuccess(p, 3, "`reg query`");
			return p.inputReader(StandardCharsets.UTF_8).lines().filter(outputFilter);
		} catch (TimeoutException | IOException | CommandFailedException e) {
			throw new RuntimeException("FAIL");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("FAIL");
		}
	}


	/**
	 * Waits {@code timeoutSeconds} seconds for {@code process} to finish with exit code {@code 0}.
	 *
	 * @param process The process to wait for
	 * @param timeoutSeconds How long to wait (in seconds)
	 * @param cmdDescription A short description of the process used to generate log and exception messages
	 * @throws TimeoutException Thrown when the process doesn't finish in time
	 * @throws InterruptedException Thrown when the thread is interrupted while waiting for the process to finish
	 * @throws CommandFailedException Thrown when the process exit code is non-zero
	 */
	@Blocking
	static void waitForSuccess(Process process, int timeoutSeconds, String cmdDescription) throws TimeoutException, InterruptedException, CommandFailedException {
		boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
		if (!exited) {
			throw new TimeoutException(cmdDescription + " timed out after " + timeoutSeconds + "s");
		}
		if (process.exitValue() != 0) {
			@SuppressWarnings("resource") var stdout = process.inputReader(StandardCharsets.UTF_8).lines().collect(Collectors.joining("\n"));
			@SuppressWarnings("resource") var stderr = process.errorReader(StandardCharsets.UTF_8).lines().collect(Collectors.joining("\n"));
			throw new CommandFailedException(cmdDescription, process.exitValue(), stdout, stderr);
		}
	}

	static class CommandFailedException extends Exception {

		int exitCode;
		String stdout;
		String stderr;

		private CommandFailedException(String cmdDescription, int exitCode, String stdout, String stderr) {
			super(cmdDescription + " returned with non-zero exit code " + exitCode);
			this.exitCode = exitCode;
			this.stdout = stdout;
			this.stderr = stderr;
		}

	}


}
