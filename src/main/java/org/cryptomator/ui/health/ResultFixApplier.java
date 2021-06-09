package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.Masterkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.scene.control.Alert;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
class ResultFixApplier {

	private static final Logger LOG = LoggerFactory.getLogger(ResultFixApplier.class);

	private final Path vaultPath;
	private final SecureRandom csprng;
	private final Masterkey masterkey;
	private final VaultConfig vaultConfig;

	@Inject
	public ResultFixApplier(@HealthCheckWindow Vault vault, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, SecureRandom csprng) {
		this.vaultPath = vault.getPath();
		this.masterkey = masterkeyRef.get();
		this.vaultConfig = vaultConfigRef.get();
		this.csprng = csprng;
	}

	public void fix(DiagnosticResult result) {
		Preconditions.checkArgument(result.getSeverity() == DiagnosticResult.Severity.WARN, "Unfixable result");
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkeyClone)) {
			result.fix(vaultPath, vaultConfig, masterkeyClone, cryptor);
		} catch (Exception e) {
			LOG.error("Failed to apply fix", e);
			Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
			alert.showAndWait();
			//TODO: real error/not supported handling
		}
	}
}
