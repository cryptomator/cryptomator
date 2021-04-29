package org.cryptomator.ui.health;

import dagger.Lazy;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;

import javax.inject.Inject;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
	private static final String REPORT_CHECK_SUCCESS = "\tCheck %s successful. Results:\n";
	private static final String REPORT_CHECK_RESULT = "\t\t %s - %s\n";
	private static final String REPORT_CHECK_CANCELED = "\tCheck %s canceled.\n";
	private static final String REPORT_CHECK_FAILED = "\tCheck %s failed.\n";

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
		var path = env.getLogDir().orElse(Path.of(System.getProperty("user.home"))).resolve("healthReport_" + vault.getDisplayName() + "(" + vaultConfig.getId() + ")" + ".log");
		final var tasks = this.tasks.get();
		//use file channel, since results can be pretty big
		try (var channel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			internalWrite(channel, String.format(REPORT_HEADER, vaultConfig.getId(), vault.getDisplayName(), vault.getPath()));
			for (var task : tasks) {
				final var state = task.getEndState();
				if (state == Worker.State.SUCCEEDED) {
					internalWrite(channel, REPORT_CHECK_SUCCESS, task.getCheck().identifier());
					for (var result : task.results()) {
						internalWrite(channel, REPORT_CHECK_RESULT, result.getServerity(), result);
					}
				} else if (state == Worker.State.CANCELLED) {
					internalWrite(channel, REPORT_CHECK_CANCELED, task.getCheck().identifier());
				} else if (state == Worker.State.FAILED) {
					internalWrite(channel, REPORT_CHECK_FAILED, task.getCheck().identifier());
					internalWrite(channel, prepareFailureMsg(task));
				} else {
					throw new IllegalStateException("Cannot export unfinished task");
				}
			}
		}
		return null;
	}

	private void internalWrite(ByteChannel channel, String s, Object... formatArguments) throws IOException {
		channel.write(ByteBuffer.wrap(s.formatted(formatArguments).getBytes(StandardCharsets.UTF_8)));
	}

	private String prepareFailureMsg(HealthCheckTask task) {
		return task.getExceptionOnDone() //
				.map(t -> ExceptionUtils.getStackTrace(t)).orElse("Unknown reason of failure.") //
				.lines().map(line -> "\t\t" + line + "\n") //
				.collect(Collectors.joining());
	}

}
