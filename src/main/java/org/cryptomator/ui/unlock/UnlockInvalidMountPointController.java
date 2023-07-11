package org.cryptomator.ui.unlock;

import org.cryptomator.common.mount.HideawayNotDirectoryException;
import org.cryptomator.common.mount.IllegalMountPointException;
import org.cryptomator.common.mount.MountPointCleanupFailedException;
import org.cryptomator.common.mount.MountPointInUseException;
import org.cryptomator.common.mount.MountPointNotEmptyDirectoryException;
import org.cryptomator.common.mount.MountPointNotExistingException;
import org.cryptomator.common.mount.MountPointNotSupportedException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FormattedLabel;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.jetbrains.annotations.PropertyKey;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

//At the current point in time only the CustomMountPointChooser may cause this window to be shown.
@UnlockScoped
public class UnlockInvalidMountPointController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final FxApplicationWindows appWindows;
	private final ResourceBundle resourceBundle;
	private final ExceptionType exceptionType;
	private final Path exceptionPath;
	private final String exceptionMessage;
	private final Path hideawayPath;

	public FormattedLabel dialogDescription;

	@Inject
	UnlockInvalidMountPointController(@UnlockWindow Stage window, @UnlockWindow Vault vault, @UnlockWindow AtomicReference<IllegalMountPointException> illegalMountPointException, FxApplicationWindows appWindows, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.appWindows = appWindows;
		this.resourceBundle = resourceBundle;

		var exc = illegalMountPointException.get();
		this.exceptionType = getExceptionType(exc);
		this.exceptionPath = exc.getMountpoint();
		this.exceptionMessage = exc.getMessage();
		this.hideawayPath = exc instanceof HideawayNotDirectoryException haeExc ? haeExc.getHideaway() : null;
	}

	@FXML
	public void initialize() {
		dialogDescription.setFormat(resourceBundle.getString(exceptionType.translationKey));
		dialogDescription.setArg1(exceptionPath);
		dialogDescription.setArg2(exceptionMessage);
		dialogDescription.setArg3(hideawayPath);
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void closeAndOpenPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.VOLUME);
		window.close();
	}

	@FXML
	public void closeAndOpenVaultOptions() {
		appWindows.showVaultOptionsWindow(vault, SelectedVaultOptionsTab.MOUNT);
		window.close();
	}

	private ExceptionType getExceptionType(Throwable unlockException) {
		return switch (unlockException) {
			case MountPointNotSupportedException x -> ExceptionType.NOT_SUPPORTED;
			case MountPointNotExistingException x -> ExceptionType.NOT_EXISTING;
			case MountPointInUseException x -> ExceptionType.IN_USE;
			case HideawayNotDirectoryException x -> ExceptionType.HIDEAWAY_NOT_DIR;
			case MountPointCleanupFailedException x -> ExceptionType.COULD_NOT_BE_CLEARED;
			case MountPointNotEmptyDirectoryException x -> ExceptionType.NOT_EMPTY_DIRECTORY;
			default -> ExceptionType.GENERIC;
		};
	}

	private enum ExceptionType {

		NOT_SUPPORTED("unlock.error.customPath.description.notSupported", ButtonAction.SHOW_PREFERENCES),
		NOT_EXISTING("unlock.error.customPath.description.notExists", ButtonAction.SHOW_VAULT_OPTIONS),
		IN_USE("unlock.error.customPath.description.inUse", ButtonAction.SHOW_VAULT_OPTIONS),
		HIDEAWAY_NOT_DIR("unlock.error.customPath.description.hideawayNotDir", ButtonAction.SHOW_VAULT_OPTIONS),
		COULD_NOT_BE_CLEARED("unlock.error.customPath.description.couldNotBeCleaned", ButtonAction.SHOW_VAULT_OPTIONS),
		NOT_EMPTY_DIRECTORY("unlock.error.customPath.description.notEmptyDir", ButtonAction.SHOW_VAULT_OPTIONS),
		GENERIC("unlock.error.customPath.description.generic", ButtonAction.SHOW_PREFERENCES);

		private final String translationKey;
		private final ButtonAction action;

		ExceptionType(@PropertyKey(resourceBundle = "i18n.strings") String translationKey, ButtonAction action) {
			this.translationKey = translationKey;
			this.action = action;
		}
	}

	private enum ButtonAction {

		//TODO Add option to show filesystem, e.g. for ExceptionType.HIDEAWAY_EXISTS
		SHOW_PREFERENCES,
		SHOW_VAULT_OPTIONS;

	}

	/* Getter */

	public boolean isShowPreferences() {
		return exceptionType.action == ButtonAction.SHOW_PREFERENCES;
	}

	public boolean isShowVaultOptions() {
		return exceptionType.action == ButtonAction.SHOW_VAULT_OPTIONS;
	}
}