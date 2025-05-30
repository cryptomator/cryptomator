package org.cryptomator.common.locationpresets;

import org.cryptomator.integrations.common.OperatingSystem;
import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOG = LoggerFactory.getLogger(OneDriveWindowsLocationPresetsProvider.class);
	private static final String REGSTR_TOKEN = "REG_SZ";
	private static final String REG_ONEDRIVE_ACCOUNTS = "HKEY_CURRENT_USER\\Software\\Microsoft\\OneDrive\\Accounts\\";

	@Override
	public Stream<LocationPreset> getLocations() {
		try {
			var accountRegKeys = queryRegistry(REG_ONEDRIVE_ACCOUNTS, List.of(), l -> l.startsWith(REG_ONEDRIVE_ACCOUNTS)).toList();
			var cloudLocations = new ArrayList<LocationPreset>();
			for (var accountRegKey : accountRegKeys) {
				var path = queryRegistry(accountRegKey, List.of("/v", "UserFolder"), l -> l.contains("UserFolder")).map(result -> result.substring(result.indexOf(REGSTR_TOKEN) + REGSTR_TOKEN.length()).trim()) //
						.map(Path::of) //
						.findFirst().orElseThrow();
				var name = "OneDrive"; //we assume personal oneDrive account by default
				if (!accountRegKey.endsWith("Personal")) {
					name = queryRegistry(accountRegKey, List.of("/v", "DisplayName"), l -> l.contains("DisplayName")).map(result -> result.substring(result.indexOf(REGSTR_TOKEN) + REGSTR_TOKEN.length()).trim()) //
							.map("OneDrive - "::concat) //
							.findFirst().orElseThrow();
				}
				cloudLocations.add(new LocationPreset(name, path));
			}
			return cloudLocations.stream();
		} catch (IOException | CommandFailedException | TimeoutException e) {
			LOG.error("Unable to determine OneDrive location", e);
			return Stream.of();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.error("Determination of OneDrive location interrupted", e);
			return Stream.of();
		}
	}

	private Stream<String> queryRegistry(String keyname, List<String> moreArgs, Predicate<String> outputFilter) throws InterruptedException, CommandFailedException, TimeoutException, IOException {
		var args = new ArrayList<String>();
		args.add("reg");
		args.add("query");
		args.add(keyname);
		args.addAll(moreArgs);
		ProcessBuilder command = new ProcessBuilder(args);
		Process p = command.start();
		waitForSuccess(p, 3, "`reg query`");
		return p.inputReader(StandardCharsets.ISO_8859_1).lines().filter(outputFilter);
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
	private static void waitForSuccess(Process process, int timeoutSeconds, String cmdDescription) throws TimeoutException, InterruptedException, CommandFailedException {
		boolean exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
		if (!exited) {
			throw new TimeoutException(cmdDescription + " timed out after " + timeoutSeconds + "s");
		}
		if (process.exitValue() != 0) {
			@SuppressWarnings("resource") var stdout = process.inputReader(StandardCharsets.ISO_8859_1).lines().collect(Collectors.joining("\n"));
			@SuppressWarnings("resource") var stderr = process.errorReader(StandardCharsets.ISO_8859_1).lines().collect(Collectors.joining("\n"));
			throw new CommandFailedException(cmdDescription, process.exitValue(), stdout, stderr);
		}
	}

	private static class CommandFailedException extends Exception {

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
