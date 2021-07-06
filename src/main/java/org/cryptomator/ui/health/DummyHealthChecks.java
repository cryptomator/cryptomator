package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.Masterkey;

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
		}
	}

	public static class DummyCheck2 implements HealthCheck {

		@Override
		public void check(Path path, VaultConfig vaultConfig, Masterkey masterkey, Cryptor cryptor, Consumer<DiagnosticResult> consumer) {
			// no-op
		}
	}

	public static class DummyCheck3 implements HealthCheck {

		@Override
		public void check(Path path, VaultConfig vaultConfig, Masterkey masterkey, Cryptor cryptor, Consumer<DiagnosticResult> consumer) {
			// no-op
		}
	}

}
