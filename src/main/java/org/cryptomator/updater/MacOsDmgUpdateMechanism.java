package org.cryptomator.updater;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.update.DownloadUpdateStep;
import org.cryptomator.integrations.update.UpdateFailedException;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.integrations.update.UpdateStep;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;


@Priority(1000)
@OperatingSystem(OperatingSystem.Value.MAC)
@DisplayName("download .dmg file") // TODO: localize
public class MacOsDmgUpdateMechanism extends DownloadUpdateMechanism {

	private static final Logger LOG = LoggerFactory.getLogger(MacOsDmgUpdateMechanism.class);

	@Override
	DownloadUpdateInfo checkForUpdate(String currentVersion, LatestVersionResponse response) {
		String suffix = switch (System.getProperty("os.arch")) {
			case "aarch64", "arm64" -> "arm64.dmg";
			default -> "x64.dmg";
		};
		var updateVersion = response.latestVersion().macVersion();
		var asset = response.assets().stream().filter(a -> a.name().endsWith(suffix)).findAny().orElse(null);
		if (UpdateMechanism.isUpdateAvailable(updateVersion, currentVersion) && asset != null) {
			return new DownloadUpdateMechanism.DownloadUpdateInfo(this, updateVersion, asset);
		} else {
			return null;
		}
	}

	@Override
	public UpdateStep firstStep(DownloadUpdateInfo updateInfo) throws UpdateFailedException {
		try {
			Path workDir = Files.createTempDirectory("cryptomator-update");
			return new UpdateProcessImpl(workDir, updateInfo);
		} catch (IOException e) {
			throw new UpdateFailedException("Failed to create temporary directory for update", e);
		}
	}

	private static class UpdateProcessImpl extends DownloadUpdateStep {

		private final Path workDir;

		public UpdateProcessImpl(Path workDir, DownloadUpdateInfo updateInfo) {
			var destination = workDir.resolve("update.dmg");
			var downloadUri = URI.create(updateInfo.asset().downloadUrl());
			var checksum = HexFormat.of().withLowerCase().parseHex(updateInfo.asset().digest().substring(7)); // remove "sha256:" prefix
			super(downloadUri, destination, checksum, 60_000_000L); // initially assume 60 MB for the update size
			this.workDir = workDir;
		}

		@Override
		public @Nullable UpdateStep nextStep() throws IllegalStateException, IOException {
			if (!isDone()) {
				throw new IllegalStateException("Update not yet downloaded");
			} else if (downloadException != null) {
				throw new UpdateFailedException("Download failed", downloadException);
			}
			return UpdateStep.of("Mounting...", this::mount);
		}

		private UpdateStep mount() throws IOException {
			// Extract Cryptomator.app from the .dmg file
			String script = """
					hdiutil attach 'update.dmg' -mountpoint "/Volumes/Cryptomator_${MOUNT_ID}" -nobrowse -quiet &&
					cp -R "/Volumes/Cryptomator_${MOUNT_ID}/Cryptomator.app" 'Cryptomator.app' &&
					hdiutil detach "/Volumes/Cryptomator_${MOUNT_ID}" -quiet
					""";
			var command = List.of("bash", "-c", script);
			var processBuilder = new ProcessBuilder(command);
			processBuilder.directory(workDir.toFile());
			processBuilder.environment().put("MOUNT_ID", UUID.randomUUID().toString());
			Process p = processBuilder.start();
			try {
				if (p.waitFor() != 0) {
					LOG.error("Failed to extract DMG, exit code: {}, output: {}", p.exitValue(), new String(p.getErrorStream().readAllBytes()));
					throw new IOException("Failed to extract DMG, exit code: " + p.exitValue());
				}
				LOG.debug("Update ready: {}", workDir.resolve("Cryptomator.app"));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Failed to extract DMG, interrupted");
			}
			return UpdateStep.of("Restarting...", this::restart);
		}

		public UpdateStep restart() throws IllegalStateException, IOException {
			String selfPath = ProcessHandle.current().info().command().orElse("");
			String installPath;
			if (selfPath.startsWith("/Applications/Cryptomator.app")) {
				installPath = "/Applications/Cryptomator.app";
			} else if (selfPath.contains("/Cryptomator.app/")) {
				installPath = selfPath.substring(0, selfPath.indexOf("/Cryptomator.app/")) + "/Cryptomator.app";
			} else {
				throw new UpdateFailedException("Cannot determine destination path for Cryptomator.app, current path: " + selfPath);
			}
			LOG.info("Restarting to apply Update in {} now...", workDir);
			String script = """
					while kill -0 ${CRYPTOMATOR_PID} 2> /dev/null; do sleep 0.5; done;
					if [ -d "${CRYPTOMATOR_INSTALL_PATH}" ]; then
					  echo "Removing old installation at ${CRYPTOMATOR_INSTALL_PATH}";
					  rm -rf "${CRYPTOMATOR_INSTALL_PATH}"
					fi
					mv 'Cryptomator.app' "${CRYPTOMATOR_INSTALL_PATH}";
					open -a "${CRYPTOMATOR_INSTALL_PATH}";
					""";
			Files.writeString(workDir.resolve("install.sh"), script, StandardCharsets.US_ASCII, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			var command = List.of("bash", "-c", "nohup bash install.sh >install.log 2>&1 &");
			var processBuilder = new ProcessBuilder(command);
			processBuilder.directory(workDir.toFile());
			processBuilder.environment().put("CRYPTOMATOR_PID", String.valueOf(ProcessHandle.current().pid()));
			processBuilder.environment().put("CRYPTOMATOR_INSTALL_PATH", installPath);
			processBuilder.start();

			return UpdateStep.EXIT;
		}
	}

}
