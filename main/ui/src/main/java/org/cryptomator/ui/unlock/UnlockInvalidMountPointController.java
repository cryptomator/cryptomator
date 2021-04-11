package org.cryptomator.ui.unlock;

import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.error.AbstractErrorController;
import org.cryptomator.ui.error.ErrorReport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javafx.scene.Scene;
import javafx.stage.Stage;

//At the current point in time only the CustomMountPointChooser may cause this window to be shown.
@UnlockScoped
public class UnlockInvalidMountPointController extends AbstractErrorController {

	private final Vault vault;

	@Inject
	UnlockInvalidMountPointController(@ErrorReport Stage window, @ErrorReport @Nullable Scene previousScene, @ErrorReport Vault vault) {
		super(window, previousScene);
		this.vault = vault;
	}

	/* Getter/Setter */

	public String getMountPoint() {
		return this.vault.getVaultSettings().getCustomMountPath().orElse("AUTO");
	}

	public boolean getMustExist() {
		MountPointRequirement requirement = this.vault.getVolume().orElseThrow(() -> new IllegalStateException("Invalid Mountpoint without a Volume?!")).getMountPointRequirement();
		assert requirement != MountPointRequirement.NONE; //An invalid MountPoint with no required MountPoint doesn't seem sensible
		assert requirement != MountPointRequirement.PARENT_OPT_MOUNT_POINT; //Not implemented anywhere (yet)

		return requirement == MountPointRequirement.EMPTY_MOUNT_POINT;
	}
}
