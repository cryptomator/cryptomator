package org.cryptomator.ui.health;

import dagger.Lazy;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;

import javax.inject.Inject;
import javafx.concurrent.Task;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
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
public class HealthReportWriteTask extends Task<Void> {

	private static final String REPORT_HEADER = """
			**************************************
			*   Cryptomator Vault Health Report  *
			**************************************
			Analyzed vault: %s (Current name \"%s\")
			Vault storage path: %s
			""";
	private static final String REPORT_CHECK_HEADER = """
			   
			   
			Check %s
			------------------------------
			""";
	private static final String REPORT_CHECK_RESULT = "%s - %s";
	private static final DateTimeFormatter TIME_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

	private final Vault vault;
	private final VaultConfig vaultConfig;
	private final Lazy<Collection<HealthCheckTask>> tasks;
	private final Environment env;

	@Inject
	public HealthReportWriteTask(@HealthCheckWindow Vault vault, AtomicReference<VaultConfig> vaultConfigRef, Lazy<Collection<HealthCheckTask>> tasks, Environment env) {
		this.vault = vault;
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.tasks = tasks;
		this.env = env;
	}

	@Override
	protected Void call() throws IOException {
		var path = env.getLogDir().orElse(Path.of(System.getProperty("user.home"))).resolve("healthReport_" + vault.getDisplayName() + "_" + TIME_STAMP.format(Instant.now()) + ".log");
		final var tasks = this.tasks.get();
		//use file channel, since results can be pretty big
		try (var channel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			internalWrite(channel, String.format(REPORT_HEADER, vaultConfig.getId(), vault.getDisplayName(), vault.getPath()));
			for (var task : tasks) {
				internalWrite(channel, REPORT_CHECK_HEADER, task.getCheck().identifier());
				final var state = task.getEndState();
				switch (state) {
					case SUCCEEDED -> {
						internalWrite(channel, "STATUS: SUCCESS\nRESULTS:\n");
						for (var result : task.results()) {
							internalWrite(channel, REPORT_CHECK_RESULT, severityToString(result.getServerity()), result);
						}
					}
					case CANCELLED -> internalWrite(channel, "STATUS: CANCELED\n");
					case FAILED -> {
						internalWrite(channel, "STATUS: FAILED\nREASON:\n", task.getCheck().identifier());
						internalWrite(channel, prepareFailureMsg(task));
					}
					case READY, RUNNING, SCHEDULED -> throw new IllegalStateException("Cannot export unfinished task");
				}
			}
		}
		return null;
	}

	private void internalWrite(ByteChannel channel, String s, Object... formatArguments) throws IOException {
		channel.write(ByteBuffer.wrap(s.formatted(formatArguments).getBytes(StandardCharsets.UTF_8)));
	}

	private String severityToString(DiagnosticResult.Severity s) {
		return switch (s) {
			case GOOD, INFO, WARN -> s.name();
			case CRITICAL -> "CRIT";
		};
	}

	private String prepareFailureMsg(HealthCheckTask task) {
		return task.getExceptionOnDone() //
				.map(t -> ExceptionUtils.getStackTrace(t)).orElse("Unknown reason of failure.") //
				.lines().map(line -> "\t\t" + line + "\n") //
				.collect(Collectors.joining());
	}

}
