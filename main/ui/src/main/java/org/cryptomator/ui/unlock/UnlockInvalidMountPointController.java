package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

//At the current point in time only the CustomMountPointChooser may cause this window to be shown.
@UnlockScoped
public class UnlockInvalidMountPointController implements FxController {

	private final Stage window;
	private final Lazy<Scene> unlockScene;
	private final Vault vault;

	@Inject
	UnlockInvalidMountPointController(@UnlockWindow Stage window, @FxmlScene(FxmlFile.UNLOCK) Lazy<Scene> unlockScene, @UnlockWindow Vault vault) {
		this.window = window;
		this.unlockScene = unlockScene;
		this.vault = vault;
	}

	@FXML
	public void back() {
		window.setScene(unlockScene.get());
	}

	/* Getter/Setter */

	public String getMountPoint() {
		return vault.getVaultSettings().getCustomMountPath().orElse("AUTO");
	}

	public boolean getMustExist() {
		MountPointRequirement requirement = vault.getVolume().orElseThrow(() -> new IllegalStateException("Invalid Mountpoint without a Volume?!")).getMountPointRequirement();
		assert requirement != MountPointRequirement.NONE; //An invalid MountPoint with no required MountPoint doesn't seem sensible
		assert requirement != MountPointRequirement.PARENT_OPT_MOUNT_POINT; //Not implemented anywhere (yet)

		return requirement == MountPointRequirement.EMPTY_MOUNT_POINT;
	}

}
