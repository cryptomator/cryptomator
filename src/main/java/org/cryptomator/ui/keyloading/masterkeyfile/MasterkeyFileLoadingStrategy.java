package org.cryptomator.ui.keyloading.masterkeyfile;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.common.BackupHelper;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class MasterkeyFileLoadingStrategy implements KeyLoadingStrategy {

	public static final String SCHEME = "masterkeyfile";

	private final Vault vault;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final Stage window;
	private final Lazy<Scene> selectMasterkeyFileScene;
	private final PassphraseEntryComponent.Builder passphraseEntry;
	private final UserInteractionLock<MasterkeyFileLoadingModule.MasterkeyFileProvision> masterkeyFileProvisionLock;
	private final AtomicReference<Path> filePath;
	private final KeychainManager keychain;

	private char[] passphrase;
	private boolean savePassphrase;
	private boolean wrongPassphrase;

	@Inject
	public MasterkeyFileLoadingStrategy(@KeyLoading Vault vault, MasterkeyFileAccess masterkeyFileAccess, @KeyLoading Stage window, @FxmlScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE) Lazy<Scene> selectMasterkeyFileScene, @Named("savedPassword") Optional<char[]> savedPassphrase, PassphraseEntryComponent.Builder passphraseEntry, UserInteractionLock<MasterkeyFileLoadingModule.MasterkeyFileProvision> masterkeyFileProvisionLock, AtomicReference<Path> filePath, KeychainManager keychain) {
		this.vault = vault;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.window = window;
		this.selectMasterkeyFileScene = selectMasterkeyFileScene;
		this.passphraseEntry = passphraseEntry;
		this.masterkeyFileProvisionLock = masterkeyFileProvisionLock;
		this.filePath = filePath;
		this.keychain = keychain;
		this.passphrase = savedPassphrase.orElse(null);
		this.savePassphrase = savedPassphrase.isPresent();
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(SCHEME.equalsIgnoreCase(keyId.getScheme()), "Only supports keys with scheme " + SCHEME);
		try {
			Path filePath = vault.getPath().resolve(keyId.getSchemeSpecificPart());
			if (!Files.exists(filePath)) {
				filePath = getAlternateMasterkeyFilePath();
			}
			if (passphrase == null) {
				askForPassphrase();
			}
			var masterkey = masterkeyFileAccess.load(filePath, CharBuffer.wrap(passphrase));
			//backup
			if (filePath.startsWith(vault.getPath())) {
				try {
					BackupHelper.attemptBackup(filePath);
				} catch (IOException e) {
					LOG.warn("Unable to create backup for masterkey file.");
				}
			} else {
				LOG.info("Masterkey file not stored inside vault. Not creating a backup.");
			}
			return masterkey;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Unlock interrupted", e);
		}
	}

	@Override
	public boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		if (exception instanceof InvalidPassphraseException) {
			this.wrongPassphrase = true;
			Arrays.fill(passphrase, '\0');
			this.passphrase = null;
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
		Arrays.fill(passphrase, '\0');
	}

	private void savePasswordToSystemkeychain(char[] passphrase) {
		if (keychain.isSupported()) {
			try {
				keychain.storePassphrase(vault.getId(), vault.getDisplayName(), CharBuffer.wrap(passphrase));
			} catch (KeychainAccessException e) {
				LOG.error("Failed to store passphrase in system keychain.", e);
			}
		}
	}

	private Path getAlternateMasterkeyFilePath() throws UnlockCancelledException, InterruptedException {
		if (filePath.get() == null) {
			return switch (askUserForMasterkeyFilePath()) {
				case MASTERKEYFILE_PROVIDED -> filePath.get();
				case CANCELED -> throw new UnlockCancelledException("Choosing masterkey file cancelled.");
			};
		} else {
			return filePath.get();
		}
	}

	private MasterkeyFileLoadingModule.MasterkeyFileProvision askUserForMasterkeyFilePath() throws InterruptedException {
		Platform.runLater(() -> {
			window.setScene(selectMasterkeyFileScene.get());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
		});
		return masterkeyFileProvisionLock.awaitInteraction();
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
