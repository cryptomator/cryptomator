package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.Masterkey;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * FIXME: Remove in production release
 */
public class DummyHealthChecks {

	public static class DummyCheck1 implements HealthCheck {

		@Override
		public void check(Path path, VaultConfig vaultConfig, Masterkey masterkey, Cryptor cryptor, Consumer<DiagnosticResult> consumer) {
			// no-op
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			consumer.accept(() -> DiagnosticResult.Severity.GOOD);
		}
	}

	public static class DummyCheck2 implements HealthCheck {

		@Override
		public void check(Path path, VaultConfig vaultConfig, Masterkey masterkey, Cryptor cryptor, Consumer<DiagnosticResult> consumer) {
			// no-op
			throw new RuntimeException("asd");
		}
	}

	public static class DummyCheck3 implements HealthCheck {

		@Override
		public void check(Path path, VaultConfig vaultConfig, Masterkey masterkey, Cryptor cryptor, Consumer<DiagnosticResult> consumer) {
			// no-op
			consumer.accept(() -> DiagnosticResult.Severity.GOOD);
			consumer.accept(() -> DiagnosticResult.Severity.CRITICAL);
			consumer.accept(() -> DiagnosticResult.Severity.INFO);
			consumer.accept(new DiagnosticResult() {
				@Override
				public Severity getSeverity() {
					return Severity.WARN;
				}

				@Override
				public void fix(Path pathToVault, VaultConfig config, Masterkey masterkey, Cryptor cryptor) throws IOException {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			consumer.accept(new DiagnosticResult() {
				@Override
				public Severity getSeverity() {
					return Severity.WARN;
				}

				@Override
				public void fix(Path pathToVault, VaultConfig config, Masterkey masterkey, Cryptor cryptor) throws IOException {
					throw new IOException("asd");
				}
			});
		}
	}

}
