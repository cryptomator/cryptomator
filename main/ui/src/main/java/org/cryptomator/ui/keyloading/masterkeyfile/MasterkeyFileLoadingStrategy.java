package org.cryptomator.ui.keyloading.masterkeyfile;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoading
public class MasterkeyFileLoadingStrategy implements KeyLoadingStrategy {

	public static final String SCHEME = "masterkeyfile";

	private final Vault vault;
	private final MasterkeyFileAccess masterkeyFileAcccess;
	private final Stage window;
	private final Lazy<Scene> passphraseEntryScene;
	private final Lazy<Scene> selectMasterkeyFileScene;
	private final UserInteractionLock<MasterkeyFileLoadingModule.PasswordEntry> passwordEntryLock;
	private final UserInteractionLock<MasterkeyFileLoadingModule.MasterkeyFileProvision> masterkeyFileProvisionLock;
	private final AtomicReference<char[]> password;
	private final AtomicReference<Path> filePath;
	private final MasterkeyFileLoadingFinisher finisher;

	private boolean wrongPassword;

	@Inject
	public MasterkeyFileLoadingStrategy(@KeyLoading Vault vault, MasterkeyFileAccess masterkeyFileAcccess, @KeyLoading Stage window, @FxmlScene(FxmlFile.UNLOCK_ENTER_PASSWORD) Lazy<Scene> passphraseEntryScene, @FxmlScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE) Lazy<Scene> selectMasterkeyFileScene, UserInteractionLock<MasterkeyFileLoadingModule.PasswordEntry> passwordEntryLock, UserInteractionLock<MasterkeyFileLoadingModule.MasterkeyFileProvision> masterkeyFileProvisionLock, AtomicReference<char[]> password, AtomicReference<Path> filePath, MasterkeyFileLoadingFinisher finisher) {
		this.vault = vault;
		this.masterkeyFileAcccess = masterkeyFileAcccess;
		this.window = window;
		this.passphraseEntryScene = passphraseEntryScene;
		this.selectMasterkeyFileScene = selectMasterkeyFileScene;
		this.passwordEntryLock = passwordEntryLock;
		this.masterkeyFileProvisionLock = masterkeyFileProvisionLock;
		this.password = password;
		this.filePath = filePath;
		this.finisher = finisher;
	}

	@Override
	public Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException {
		Preconditions.checkArgument(SCHEME.equalsIgnoreCase(keyId.getScheme()), "Only supports keys with scheme " + SCHEME);

		try {
			Path filePath = vault.getPath().resolve(keyId.getSchemeSpecificPart());
			if (!Files.exists(filePath)) {
				filePath = getAlternateMasterkeyFilePath();
			}
			CharSequence passphrase = getPassphrase();
			return masterkeyFileAcccess.load(filePath, passphrase);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Unlock interrupted", e);
		}
	}

	@Override
	public boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		if (exception instanceof InvalidPassphraseException) {
			this.wrongPassword = true;
			password.set(null);
			return true; // reattempting key load
		} else {
			return false; // nothing we can do
		}
	}

	@Override
	public void cleanup(boolean unlockedSuccessfully) {
		finisher.cleanup(unlockedSuccessfully);
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

	private CharSequence getPassphrase() throws UnlockCancelledException, InterruptedException {
		if (password.get() == null) {
			return switch (askForPassphrase()) {
				case PASSWORD_ENTERED -> CharBuffer.wrap(password.get());
				case CANCELED -> throw new UnlockCancelledException("Password entry cancelled.");
			};
		} else {
			// e.g. pre-filled from keychain or previous unlock attempt
			return CharBuffer.wrap(password.get());
		}
	}

	private MasterkeyFileLoadingModule.PasswordEntry askForPassphrase() throws InterruptedException {
		Platform.runLater(() -> {
			window.setScene(passphraseEntryScene.get());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
			if (wrongPassword) {
				Animations.createShakeWindowAnimation(window).play();
			}
		});
		return passwordEntryLock.awaitInteraction();
	}

}
