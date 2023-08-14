package org.cryptomator.ui.unlock;

import org.cryptomator.common.ObservableUtil;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.ResourceBundle;

//At the current point in time only the CustomMountPointChooser may cause this window to be shown.
@UnlockScoped
public class UnlockInvalidMountPointController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final FxApplicationWindows appWindows;

	private final ObservableValue<ExceptionType> exceptionType;
	private final ObservableValue<Path> exceptionPath;
	private final ObservableValue<String> exceptionMessage;
	private final ObservableValue<Path> hideawayPath;
	private final ObservableValue<String> format;
	private final ObservableValue<Boolean> showPreferences;
	private final ObservableValue<Boolean> showVaultOptions;

	public FormattedLabel dialogDescription;

	@Inject
	UnlockInvalidMountPointController(@UnlockWindow Stage window, @UnlockWindow Vault vault, @UnlockWindow ObjectProperty<IllegalMountPointException> illegalMountPointException, FxApplicationWindows appWindows, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.appWindows = appWindows;

		this.exceptionType = illegalMountPointException.map(this::getExceptionType);
		this.exceptionPath = illegalMountPointException.map(IllegalMountPointException::getMountpoint);
		this.exceptionMessage = illegalMountPointException.map(IllegalMountPointException::getMessage);
		this.hideawayPath = illegalMountPointException.map(e -> e instanceof HideawayNotDirectoryException haeExc ? haeExc.getHideaway() : null);

		this.format = ObservableUtil.mapWithDefault(exceptionType, type -> resourceBundle.getString(type.translationKey), "");
		this.showPreferences = ObservableUtil.mapWithDefault(exceptionType, type -> type.action == ButtonAction.SHOW_PREFERENCES, false);
		this.showVaultOptions = ObservableUtil.mapWithDefault(exceptionType, type -> type.action == ButtonAction.SHOW_VAULT_OPTIONS, false);
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

	public Path getExceptionPath() {
		return exceptionPath.getValue();
	}

	public ObservableValue<Path> exceptionPathProperty() {
		return exceptionPath;
	}

	public String getFormat() {
		return format.getValue();
	}

	public ObservableValue<String> formatProperty() {
		return format;
	}

	public String getExceptionMessage() {
		return exceptionMessage.getValue();
	}

	public ObservableValue<String> exceptionMessageProperty() {
		return exceptionMessage;
	}

	public Path getHideawayPath() {
		return hideawayPath.getValue();
	}

	public ObservableValue<Path> hideawayPathProperty() {
		return hideawayPath;
	}

	public Boolean getShowPreferences() {
		return showPreferences.getValue();
	}

	public ObservableValue<Boolean> showPreferencesProperty() {
		return showPreferences;
	}

	public Boolean getShowVaultOptions() {
		return showVaultOptions.getValue();
	}

	public ObservableValue<Boolean> showVaultOptionsProperty() {
		return showVaultOptions;
	}
}