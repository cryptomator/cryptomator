package org.cryptomator.ui.unlock;

import dagger.Lazy;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;

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

}
