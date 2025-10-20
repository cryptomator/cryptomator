package org.cryptomator.ui.keyloading.masterkeyfile;

import com.google.common.base.Preconditions;
import org.cryptomator.common.Passphrase;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultIdentity;
import org.cryptomator.cryptofs.common.BackupHelper;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@KeyLoading
public class MasterkeyFileLoadingStrategy implements KeyLoadingStrategy {
	
	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyFileLoadingStrategy.class);

	public static final String SCHEME = "masterkeyfile";

	private final Vault vault;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final MultiKeyslotFile multiKeyslotFile;
	private final Stage window;
	private final PassphraseEntryComponent.Builder passphraseEntry;
	private final ChooseMasterkeyFileComponent.Builder masterkeyFileChoice;
	private final KeychainManager keychain;
	private final ResourceBundle resourceBundle;
	private final ObjectProperty<VaultIdentity> selectedIdentity;

	private Passphrase passphrase;
	private boolean savePassphrase;
	private boolean wrongPassphrase;

	@Inject
	public MasterkeyFileLoadingStrategy(@KeyLoading Vault vault, MasterkeyFileAccess masterkeyFileAccess, MultiKeyslotFile multiKeyslotFile, @KeyLoading Stage window, @Named("savedPassword") Optional<char[]> savedPassphrase, PassphraseEntryComponent.Builder passphraseEntry, ChooseMasterkeyFileComponent.Builder masterkeyFileChoice, KeychainManager keychain, ResourceBundle resourceBundle) {
		this.vault = vault;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.multiKeyslotFile = multiKeyslotFile;
		this.window = window;
		this.passphraseEntry = passphraseEntry;
		this.masterkeyFileChoice = masterkeyFileChoice;
		this.keychain = keychain;
		this.resourceBundle = resourceBundle;
		this.selectedIdentity = new SimpleObjectProperty<>();
		this.passphrase = savedPassphrase.map(Passphrase::new).orElse(null);
		this.savePassphrase = savedPassphrase.isPresent();
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		window.setTitle(resourceBundle.getString("unlock.title").formatted(vault.getDisplayName()));
		Preconditions.checkArgument(SCHEME.equalsIgnoreCase(keyId.getScheme()), "Only supports keys with scheme " + SCHEME);
		try {
			// Get password from user if not already provided
			if (passphrase == null) {
				askForPassphrase();
			}
			
			// Determine masterkey path from keyId, or fallback to user selection
			Path masterkeyPath;
			String keyIdPath = keyId.getSchemeSpecificPart();
			if (keyIdPath != null && !keyIdPath.isEmpty() && !keyIdPath.equals("masterkey.cryptomator")) {
				// keyId specifies a specific path
				masterkeyPath = vault.getPath().resolve(keyIdPath);
			} else {
				// Default path
				masterkeyPath = vault.getPath().resolve("masterkey.cryptomator");
			}
			
			if (!Files.exists(masterkeyPath)) {
				// Masterkey file not found, ask user to choose
				LOG.debug("Masterkey file not found at {}, asking user to choose", masterkeyPath);
				masterkeyPath = askUserForMasterkeyFilePath();
			}
			
		// MultiKeyslotFile.load() automatically tries all keyslots with the password
		// Whichever keyslot decrypts successfully is used (user never knows which)
		Masterkey masterkey = multiKeyslotFile.load(masterkeyPath, passphrase);
		LOG.debug("Successfully loaded master key");
			
			// Backup the masterkey file
			try {
				BackupHelper.attemptBackup(masterkeyPath);
			} catch (IOException e) {
				LOG.warn("Unable to create backup for masterkey file.");
			}
			
		// Detect which identity was used based on the masterkey
		detectUsedIdentity();
		
		return masterkey;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Unlock interrupted", e);
		} catch (InvalidPassphraseException e) {
			throw new MasterkeyLoadingFailedException("Wrong password", e);
		} catch (IOException e) {
			throw new MasterkeyLoadingFailedException("Failed to load masterkey", e);
		}
	}

	/**
	 * Identity is intentionally unknown by design for plausible deniability.
	 * We cannot and should not try to detect which keyslot was used.
	 */
	private void detectUsedIdentity() {
		// Identity intentionally unknown by design for plausible deniability.
		selectedIdentity.set(null);
	}

	public VaultIdentity getSelectedIdentity() {
		return selectedIdentity.get();
	}

	@Override
	public boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		if (exception.getCause() instanceof InvalidPassphraseException) {
			this.wrongPassphrase = true;
			if (passphrase != null) {
				passphrase.destroy();
				this.passphrase = null;
			}
			return true; // reattempting key load
		} else {
			return false; // nothing we can do
		}
	}

	@Override
	public void cleanup(boolean unlockedSuccessfully) {
		if (unlockedSuccessfully && savePassphrase) {
			savePasswordToSystemkeychain(passphrase);
		}
		if (passphrase != null) {
			passphrase.destroy();
		}
	}

	private void savePasswordToSystemkeychain(Passphrase passphrase) {
		try {
			// Don't save passwords for multi-identity vaults
			// Each identity may have a different password
			var manager = vault.getIdentityProvider().getManager();
			if (manager.getIdentities().size() > 1) {
				// Intentionally avoid logging identity-related details
				return;
			}
			
			if (keychain.isSupported() && !keychain.getPassphraseStoredProperty(vault.getId()).get()) {
				keychain.storePassphrase(vault.getId(), vault.getDisplayName(), passphrase);
			}
		} catch (KeychainAccessException e) {
			LOG.error("Failed to store passphrase in system keychain.", e);
		} catch (Exception e) {
			LOG.warn("Failed to check identity count when saving password", e);
		}
	}

	private Path askUserForMasterkeyFilePath() throws InterruptedException {
		var comp = masterkeyFileChoice.build();
		Platform.runLater(() -> {
			window.setScene(comp.chooseMasterkeyScene());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
		});
		try {
			return comp.result().get();
		} catch (CancellationException e) {
			throw new UnlockCancelledException("Choosing masterkey file cancelled.");
		} catch (ExecutionException e) {
			throw new MasterkeyLoadingFailedException("Failed to select masterkey file.", e);
		}
	}

	private void askForPassphrase() throws InterruptedException {
		var comp = passphraseEntry.savedPassword(passphrase).build();
		Platform.runLater(() -> {
			window.setScene(comp.passphraseEntryScene());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
			if (wrongPassphrase) {
				Animations.createShakeWindowAnimation(window).play();
			}
		});
		try {
			var result = comp.result().get();
			this.passphrase = result.passphrase();
			this.savePassphrase = result.savePassphrase();
		} catch (CancellationException e) {
			throw new UnlockCancelledException("Password entry cancelled.");
		} catch (ExecutionException e) {
			throw new MasterkeyLoadingFailedException("Failed to ask for password.", e);
		}
	}

}
