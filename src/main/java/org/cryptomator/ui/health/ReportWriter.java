package org.cryptomator.ui.health;

import com.google.common.base.Throwables;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;

import javax.inject.Inject;
import javafx.application.Application;
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

	private static final String REPORT_HEADER = """
			*******************************************
			*     Cryptomator Vault Health Report     *
			*******************************************
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
	private final Application application;
	private final Path exportDestination;

	@Inject
	public ReportWriter(@HealthCheckWindow Vault vault, AtomicReference<VaultConfig> vaultConfigRef, Application application, Environment env) {
		this.vault = vault;
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.application = application;
		this.exportDestination = env.getLogDir().orElse(Path.of(System.getProperty("user.home"))).resolve("healthReport_" + vault.getDisplayName() + "_" + TIME_STAMP.format(Instant.now()) + ".log");
	}

	protected void writeReport(Collection<Check> performedChecks) throws IOException {
		try (var out = Files.newOutputStream(exportDestination, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING); //
			 var writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
			writer.write(REPORT_HEADER.formatted(vaultConfig.getId(), vault.getDisplayName(), vault.getPath()));
			for (var check : performedChecks) {
				writer.write(REPORT_CHECK_HEADER.formatted(check.getHealthCheck().name()));
				switch (check.getState()) {
					case SUCCEEDED -> {
						writer.write("STATUS: SUCCESS\nRESULTS:\n");
						for (var result : check.getResults()) {
							writer.write(REPORT_CHECK_RESULT.formatted(result.diagnosis().getSeverity(), result.getDescription()));
						}
					}
					case CANCELLED -> writer.write("STATUS: CANCELED\n");
					case ERROR -> {
						writer.write("STATUS: FAILED\nREASON:\n");
						writer.write(prepareFailureMsg(check));
					}
					case RUNNABLE, RUNNING, SCHEDULED -> throw new IllegalStateException("Checks are still running.");
					case SKIPPED -> {} //noop
				}
			}
		}
		reveal();
	}

	private String prepareFailureMsg(Check check) {
		if (check.getError() != null) {
			return Throwables.getStackTraceAsString(check.getError()) //
					.lines() //
					.map(line -> "\t\t" + line + "\n") //
					.collect(Collectors.joining());
		} else {
			return "Unknown reason of failure.";
		}
	}

	private void reveal() {
		application.getHostServices().showDocument(exportDestination.getParent().toUri().toString());
	}

}
