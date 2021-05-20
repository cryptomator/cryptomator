package org.cryptomator.ui.health;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.ui.common.HostServiceRevealer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.concurrent.Worker;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@HealthCheckScoped
public class ReportWriter {

	private static final Logger LOG = LoggerFactory.getLogger(ReportWriter.class);
	private static final String REPORT_HEADER = """
			**************************************
			*   Cryptomator Vault Health Report  *
			**************************************
			Analyzed vault: %s (Current name "%s")
			Vault storage path: %s
			""";
	private static final String REPORT_CHECK_HEADER = """
			   
			   
			Check %s
			------------------------------
			""";
	private static final String REPORT_CHECK_RESULT = "%8s - %s\n";
	private static final DateTimeFormatter TIME_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

	private final Vault vault;
	private final VaultConfig vaultConfig;
	private final Path path;
	private final HostServiceRevealer revealer;

	@Inject
	public ReportWriter(@HealthCheckWindow Vault vault, AtomicReference<VaultConfig> vaultConfigRef, Environment env, HostServiceRevealer revealer) {
		this.vault = vault;
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.revealer = revealer;
		this.path = env.getLogDir().orElse(Path.of(System.getProperty("user.home"))).resolve("healthReport_" + vault.getDisplayName() + "_" + TIME_STAMP.format(Instant.now()) + ".log");
	}

	protected void writeReport(Collection<HealthCheckTask> tasks) throws IOException {
		try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING); //
			 var writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			writer.write(REPORT_HEADER.formatted(vaultConfig.getId(), vault.getDisplayName(), vault.getPath()));
			for (var task : tasks) {
				if (task.getState() == Worker.State.READY) {
					LOG.debug("Skipping not performed check {}.",task.getCheck().identifier());
					continue;
				}
				writer.write(REPORT_CHECK_HEADER.formatted(task.getCheck().identifier()));
				switch (task.getState()) {
					case SUCCEEDED -> {
						writer.write("STATUS: SUCCESS\nRESULTS:\n");
						for (var result : task.results()) {
							writer.write(REPORT_CHECK_RESULT.formatted(result.getSeverity(), result.getDescription()));
						}
					}
					case CANCELLED -> writer.write("STATUS: CANCELED\n");
					case FAILED -> {
						writer.write("STATUS: FAILED\nREASON:\n" + task.getCheck().identifier());
						writer.write(prepareFailureMsg(task));
					}
					case RUNNING, SCHEDULED -> throw new IllegalStateException("Checks are still running.");
				}
			}
		}
		reveal();
	}

	private String prepareFailureMsg(HealthCheckTask task) {
		if (task.getException() != null) {
			return ExceptionUtils.getStackTrace(task.getException()) //
					.lines() //
					.map(line -> "\t\t" + line + "\n") //
					.collect(Collectors.joining());
		} else {
			return "Unknown reason of failure.";
		}
	}

	private void reveal() {
		try {
			revealer.reveal(path);
		} catch (Volume.VolumeException e) {
			//should not happen
		}
	}

}
