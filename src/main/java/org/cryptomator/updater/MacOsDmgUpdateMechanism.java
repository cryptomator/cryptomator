package org.cryptomator.updater;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;


@Priority(1000)
@OperatingSystem(OperatingSystem.Value.MAC)
@DisplayName("download .dmg file") // TODO: localize
public class MacOsDmgUpdateMechanism extends DownloadUpdateMechanism {

	private static final Logger LOG = LoggerFactory.getLogger(MacOsDmgUpdateMechanism.class);

	@Override
	public UpdateProcess prepareUpdate() throws IOException {
		Path workDir = Files.createTempDirectory("cryptomator-update");
		return new UpdateProcessImpl(workDir);
	}

	private static class UpdateProcessImpl extends DownloadUpdateProcess {

		// FIXME: use URI and CHECKSUM from update API
		private static final URI UPDATE_URI = URI.create("https://github.com/cryptomator/cryptomator/releases/download/1.17.0/Cryptomator-1.17.0-arm64.dmg");
		private static final byte[] CHECKSUM = HexFormat.of().withLowerCase().parseHex("03f45e203204e93b39925cbb04e19c9316da4f77debaba4fb5071f0ec8e727e8");

		public UpdateProcessImpl(Path workDir) {
			super(workDir, UPDATE_URI, CHECKSUM,60_000_000L); // initially assume 60 MB for the update size
		}

		@Override
		protected void postDownload(Path downloadedFile) throws IOException {
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
			} catch (InterruptedException e) {
				throw new InterruptedIOException("Failed to extract DMG, interrupted");
			}
		}

		@Override
		public Process applyUpdate() throws IllegalStateException, IOException {
			if (downloadException != null) {
				throw new IllegalStateException("Downloading update failed", downloadException);
			} else if (!downloadSuccessful) {
				throw new IllegalStateException("Update not yet downloaded");
			}
			// TODO: use /Applications/Cryptomator.app or ~/Applications/Cryptomator.app depending on the path of the current process (ProcessHandle.current().info().command()?)
			String script = """
					while kill -0 ${CRYPTOMATOR_PID} 2> /dev/null; do sleep 0.5; done;
					cp -R 'Cryptomator.app' '/Applications/Cryptomator.app';
					open -a '/Applications/Cryptomator.app'
					""";
			var command = List.of("bash", "-c", "nohup bash -c \"" + script + "\" >/Users/sebastian/Downloads/nohup.out 2>&1 &");
			var processBuilder = new ProcessBuilder(command);
			processBuilder.directory(workDir.toFile());
			processBuilder.environment().put("CRYPTOMATOR_PID", String.valueOf(ProcessHandle.current().pid()));
			return processBuilder.start();
		}
	}


}
