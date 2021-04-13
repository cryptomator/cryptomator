package org.cryptomator.ui.health;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptofs.health.api.HealthCheck;
import org.cryptomator.cryptolib.api.Masterkey;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@HealthCheckScoped
class HealthCheckTaskFactory {

	private final Vault vault;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;
	private final SecureRandom csprng;

	@Inject
	public HealthCheckTaskFactory(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vault = vault;
		this.masterkey = Objects.requireNonNull(masterkeyRef.get());
		this.vaultConfig = Objects.requireNonNull(vaultConfigRef.get());
		this.csprng = csprng;
	}

	public HealthCheckTask newTask(HealthCheck healthCheck, Consumer<DiagnosticResult> resultConsumer) {
		return new HealthCheckTask(vault.getPath(), vaultConfig, masterkey, csprng, healthCheck, resultConsumer);
	}

}
