package org.cryptomator.ui.unlock.masterkeyfile;

import dagger.Lazy;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileLoaderContext;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.unlock.UnlockCancelledException;
import org.cryptomator.ui.unlock.masterkeyfile.MasterkeyFileLoadingModule.MasterkeyFileProvision;
import org.cryptomator.ui.unlock.masterkeyfile.MasterkeyFileLoadingModule.PasswordEntry;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@MasterkeyFileLoadingScoped
public class MasterkeyFileLoadingContext implements MasterkeyFileLoaderContext {

	private final Stage window;
	private final Lazy<Scene> passphraseEntryScene;
	private final Lazy<Scene> selectMasterkeyFileScene;
	private final UserInteractionLock<PasswordEntry> passwordEntryLock;
	private final UserInteractionLock<MasterkeyFileProvision> masterkeyFileProvisionLock;
	private final AtomicReference<char[]> password;
	private final AtomicReference<Path> filePath;

	private boolean wrongPassword;

	@Inject
	public MasterkeyFileLoadingContext(@MasterkeyFileLoading Stage window, @FxmlScene(FxmlFile.UNLOCK_ENTER_PASSWORD) Lazy<Scene> passphraseEntryScene, @FxmlScene(FxmlFile.UNLOCK_SELECT_MASTERKEYFILE) Lazy<Scene> selectMasterkeyFileScene, UserInteractionLock<PasswordEntry> passwordEntryLock, UserInteractionLock<MasterkeyFileProvision> masterkeyFileProvisionLock, AtomicReference<char[]> password, AtomicReference<Path> filePath) {
		this.window = window;
		this.passphraseEntryScene = passphraseEntryScene;
		this.selectMasterkeyFileScene = selectMasterkeyFileScene;
		this.passwordEntryLock = passwordEntryLock;
		this.masterkeyFileProvisionLock = masterkeyFileProvisionLock;
		this.password = password;
		this.filePath = filePath;
	}

	@Override
	public Path getCorrectMasterkeyFilePath(String masterkeyFilePath) {
		if (filePath.get() != null) { // e.g. already chosen on previous attempt with wrong password
			return filePath.get();
		}

		assert filePath.get() == null;
		try {
			if (askForCorrectMasterkeyFile() == MasterkeyFileProvision.MASTERKEYFILE_PROVIDED) {
				return filePath.get();
			} else {
				throw new UnlockCancelledException("Choosing masterkey file cancelled.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Choosing masterkey file interrupted", e);
		}
	}

	private MasterkeyFileProvision askForCorrectMasterkeyFile() throws InterruptedException {
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

	@Override
	public CharSequence getPassphrase(Path path) throws UnlockCancelledException {
		if (password.get() != null) { // e.g. pre-filled from keychain
			return CharBuffer.wrap(password.get());
		}

		assert password.get() == null;
		try {
			if (askForPassphrase() == PasswordEntry.PASSWORD_ENTERED) {
				assert password.get() != null;
				return CharBuffer.wrap(password.get());
			} else {
				throw new UnlockCancelledException("Password entry cancelled.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UnlockCancelledException("Password entry interrupted", e);
		}
	}

	private PasswordEntry askForPassphrase() throws InterruptedException {
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

	public boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		if (exception instanceof InvalidPassphraseException) {
			this.wrongPassword = true;
			password.set(null);
			return true; // reattempting key load
		} else {
			return false; // nothing we can do
		}
	}
}
