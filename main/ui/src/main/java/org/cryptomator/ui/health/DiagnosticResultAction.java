package org.cryptomator.ui.health;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.health.api.DiagnosticResult;
import org.cryptomator.cryptolib.api.Masterkey;

import javafx.scene.control.Alert;
import java.nio.file.Path;
import java.security.SecureRandom;

class DiagnosticResultAction implements Runnable {

	private final DiagnosticResult result;
	private final Path vaultPath;
	private final VaultConfig vaultConfig;
	private final Masterkey masterkey;
	private final SecureRandom csprng;

	DiagnosticResultAction(DiagnosticResult result, Path vaultPath, VaultConfig vaultConfig, Masterkey masterkey, SecureRandom csprng) {
		this.result = result;
		this.vaultPath = vaultPath;
		this.vaultConfig = vaultConfig;
		this.masterkey = masterkey;
		this.csprng = csprng;
	}

	public void run() {
		try (var masterkeyClone = masterkey.clone(); //
			 var cryptor = vaultConfig.getCipherCombo().getCryptorProvider(csprng).withKey(masterkeyClone)) {
			result.fix(vaultPath, vaultConfig, masterkeyClone, cryptor);
		} catch (Exception e) {
			e.printStackTrace();
			Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
			alert.showAndWait();
			//TODO: real error/not supported handling
		}
	}

	public DiagnosticResult.Severity getSeverity() {
		return result.getServerity(); //TODO: fix spelling with updated cryptofs release
	}

	public String getDescription() {
		return result.toString();
	}

}
